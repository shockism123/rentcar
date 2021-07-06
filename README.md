<img src="https://user-images.githubusercontent.com/84000922/124483679-94491980-dde5-11eb-9304-8a559c93670a.png" alt="sk렌터카 이미지" style="zoom:80%;" />



# 서비스 시나리오

### 기능적 요구사항

```
• 관리자는 렌트카를 등록한다. 
• 고객은 렌트카를 예약한다. 
• 관리자는 렌트카 예약을 접수한다.
• 관리자는 렌트카를 배차한다. 
• 고객은 렌트카 예약을 취소할 수 있다. 
• 예약이 취소되면 배차가 취소된다. 
• 고객은 렌트현황정보를 확인할 수 있다. 
```

### 비기능적 요구사항

```
1. 트랜잭션
 - 배차 가능한 렌트카가 있어야 렌트카 예약이 가능하다. (Sync 호출)
2. 장애격리
  - 배차서비스가 수행되지 않더라도 예약은 365일 24시간 받을 수 있어야 한다. (Async (event-driven), Eventual Consistency)
  - 예약서비스가 과중되면 사용자를 잠시 동안 받지 않고 예약을 잠시후에 하도록 유도한다. (Circuit breaker, fallback)
3. 성능
 - 고객은 렌트현황조회 화면에서 상태를 확인할 수 있어야 한다. (CQRS)
```

### Microservice명

```
렌트카관리 – car
예약 - reservation
배차 - renatal
렌트현황조회 - MyPage
```



# 분석/설계

1. Evnent 도출 및 부적격 Enent 탈락(UI 이벤트 등의 성격을 제외)
2. Actor, command 부착 및 Aggregate으로 묶음
3. Bounded Context로 묶음
4. Policy 부착, 컨텍스트 매핑(점선은 Pub/Sub, 실선은 Req/Resp)
5. 기능, 비기능 요구사항 검증

### 도출된 이벤트스토밍 모형

![00  이벤트스토밍 모형](https://user-images.githubusercontent.com/84000922/124545385-e892de80-de63-11eb-8b4c-6bf438410bb4.png)

### 헥사고날 아키텍처 다이어그램 도출

![00  헥사고날 아키텍처](https://user-images.githubusercontent.com/84000922/124546197-3e1bbb00-de65-11eb-889e-6f072b9d1ff8.png)



### Git Organization / Repositories

![00  Github 등록](https://user-images.githubusercontent.com/84000922/124545609-52ab8380-de64-11eb-9ff6-a534ed2ab00f.png)





# 구현 

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라,구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각각의 포트넘버는 8081 ~ 8084, 8088 이다)

```
cd car
mvn spring-boot:run

cd gateway
mvn spring-boot:run 

cd mypage 
mvn spring-boot:run 

cd rental 
mvn spring-boot:run

cd reservation
mvn spring-boot:run 
```

### DDD(Domain-Driven-Design)의 적용

msaez.io 를 통해 구현한 Aggregate 단위로 Entity 를 선언 후, 구현을 진행함. Entity Pattern 과 Repository Pattern을 적용하기 위해 Spring Data REST 의 RestRepository 를 적용

##### 배차서비스의 Rental.java (Entity)

```
package rentcar;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;

@Entity
@Table(name="Rental_table")
public class Rental {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long rentalId;
    private Long reservationId;
    private Long carId;
    private String userId;
    private String status;

    @PostPersist
    public void onPostPersist(){
        CarAllocated carAllocated = new CarAllocated();
        BeanUtils.copyProperties(this, carAllocated);
        carAllocated.publishAfterCommit();


    }

    @PostUpdate
    public void onPostUpdate(){
        AllocationCancelled allocationCancelled = new AllocationCancelled();
        BeanUtils.copyProperties(this, allocationCancelled);
        allocationCancelled.publishAfterCommit();


        UseCompleted useCompleted = new UseCompleted();
        BeanUtils.copyProperties(this, useCompleted);
        useCompleted.publishAfterCommit();


    }


    public Long getRentalId() {
        return rentalId;
    }

    public void setRentalId(Long rentalId) {
        this.rentalId = rentalId;
    }
    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }
    public Long getCarId() {
        return carId;
    }

    public void setCarId(Long carId) {
        this.carId = carId;
    }
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
```

##### 배차서비스의 RentalRepository.java

```
package rentcar;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="rentals", path="rentals")
public interface RentalRepository extends PagingAndSortingRepository<Rental, Long>{
    Rental findByRentalId(Long rentalId);

}
```

##### 배차서비스의 PolicyHander.java

```
package rentcar;

import rentcar.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired RentalRepository rentalRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverReservationCancelled_CancelAllocation(@Payload ReservationCancelled reservationCancelled){

        if(!reservationCancelled.validate()) return;

        System.out.println("\n\n##### listener CancelAllocation : " + reservationCancelled.toJson() + "\n\n");

        // Sample Logic //
        Rental rental = new Rental();
        rental.setReservationId(reservationCancelled.getReservationId());
        rental.setCarId(reservationCancelled.getCarId());
        rental.setUserId(reservationCancelled.getUserId());
        rental.setStatus(reservationCancelled.getStatus());
        rentalRepository.save(rental);
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCarReserved_AllocateCar(@Payload CarReserved carReserved){

        if(!carReserved.validate()) return;

        System.out.println("\n\n##### listener AllocateCar : " + carReserved.toJson() + "\n\n");

        // Sample Logic //
        Rental rental = new Rental();
        rental.setReservationId(carReserved.getReserationId());
        rental.setCarId(carReserved.getCarId());
        rental.setUserId(carReserved.getUserId());
        rental.setStatus(carReserved.getStatus());
        rentalRepository.save(rental);
            
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
```

### Gateway

API GateWay를 통하여 마이크로 서비스들의 진입점을 통일

```
server:
  port: 8088

---

spring:
  profiles: default
  cloud:
    gateway:
      routes:
        - id: car
          uri: http://localhost:8081
          predicates:
            - Path=/cars/** 
        - id: reservation
          uri: http://localhost:8082
          predicates:
            - Path=/reservations/** 
        - id: rental
          uri: http://localhost:8083
          predicates:
            - Path=/rentals/** 
        - id: mypage
          uri: http://localhost:8084
          predicates:
            - Path= /mypages/**
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true


---

spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: car
          uri: http://car:8080
          predicates:
            - Path=/cars/** 
        - id: reservation
          uri: http://reservation:8080
          predicates:
            - Path=/reservations/** 
        - id: rental
          uri: http://rental:8080
          predicates:
            - Path=/rentals/** 
        - id: mypage
          uri: http://mypage:8080
          predicates:
            - Path= /mypages/**
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true

server:
  port: 8080
```

렌트카관리(car)서비스의 Gateway 적용

<img src="https://user-images.githubusercontent.com/84000922/124528908-55967c00-de44-11eb-8581-e54faaf3c8fa.png" alt="1  gateway적용" style="zoom:100%;" align=left />

### Polyglot

Mypage 서비스는 HSQLDB를 사용하기위해 pom.xml 파일에 아래 설정을 추가하였음 (다른 서비스들은 H2DB 사용)

```
<dependency>
	<groupId>org.hsqldb</groupId>
    	<artifactId>hsqldb</artifactId>
	<scope>runtime</scope>
</dependency>
```

Mypage에 렌트카예약상태가 저장됨

![2  mypage 저장 확인](https://user-images.githubusercontent.com/84000922/124529494-af4b7600-de45-11eb-8df2-36cdf129ed91.png)

### 동기식 호출과 Fallback 처리

분석단계에서의 조건 중 하나로 배차 가능한 렌트카가 있어야 렌트카 예약이 가능하며, 예약(reservation)->(car) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다.

- (동기호출-Req) reservation 서비스 내 external.CarService.java

```
package rentcar.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(name="car", url="http://${api.url.rentcar}:8080", fallback = CarServiceFallback.class)
public interface CarService {

    @RequestMapping(method= RequestMethod.GET, path="/cars/checkUpdateStock")
    public boolean checkUpdateStock(@RequestParam("carId") Long carId);

}
```

- reservation 서비스 내 Reservation.java

```
    @PostPersist
    public void onPostPersist() throws Exception{
        if (ReservationApplication.applicationContext.getBean(rentcar.external.CarService.class).checkUpdateStock(this.carId)){
            CarReserved carReserved = new CarReserved();
            BeanUtils.copyProperties(this, carReserved);
            carReserved.publishAfterCommit();
        }        
        else {
            throw new Exception("stock information is not updated");
        }
   }
```

- (동기호출-Res) car 서비스 내 Feign Client 요청 대상 (렌트가 재고 체크 및 업데이트)

```
package rentcar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

 @RestController
 public class CarController {

    @Autowired
    CarRepository carRepository;

    @RequestMapping(value = "/cars/checkUpdateStock",
       method = RequestMethod.GET,
       produces = "application/json;charset=UTF-8")
    public boolean checkUpdateStock(HttpServletRequest request, HttpServletResponse response) {
       boolean status = false;

       Long carId = Long.valueOf(request.getParameter("carId"));

        try {
            Thread.sleep((long) (800 + Math.random() * 300));
        } catch (InterruptedException e) {
        e.printStackTrace();
        }

       Car car = carRepository.findByCarId(carId);
 
        //재고차량 있는지 확인 
        if(car.getQty() > 0) {
            //렌트 가능하면 렌트처리하여 수량 감소
            status = true;
            car.setQty(car.getQty() - 1);
            carRepository.save(car);
        }    

       return status;
    }
 }
```

- (Fallback) reservation 서비스 내 CarServiceFallback.java

```
package rentcar.external;

import org.springframework.stereotype.Component;

@Component
public class CarServiceFallback implements CarService{

    @Override
    public boolean checkUpdateStock(Long carId){
        System.out.println("★★★★★★★★★★★Circuit breaker has been opened. Fallback returned instead.★★★★★★★★★★★");
        return false;
    }
}
```

```
feign:
  hystrix:
    enabled: true
```



### 적용 후 테스트

##### 렌트카관리 서비스에서 렌트카를 등록

![1 car에 차량 및 수량 등록](https://user-images.githubusercontent.com/84000922/124538889-600e4100-de57-11eb-8f19-42baada40fae.png)

##### 예약서비스에서 렌트카를 예약

![2  reservation에서 예약 등록](https://user-images.githubusercontent.com/84000922/124538912-6bfa0300-de57-11eb-8f86-17c620105104.png)

##### 예약서비스에서 렌트카를 예약 > 배차 DB에 자동 등록됨 (Async - Policy)

![3  rental에 같이 등록](https://user-images.githubusercontent.com/84000922/124539051-b11e3500-de57-11eb-84c2-5ecd6609ba43.png)

##### MyPage-렌탈현황조회(CQRS) 

![3  mypage에서 같이 등록](https://user-images.githubusercontent.com/84000922/124539101-c5623200-de57-11eb-9f6e-e8d80e86ea2f.png)

##### 예약서비스에서 렌트카를 예약 > 재고 체크 후 자동갱신됨 (Sync - Req/Res)

![4  car에 동기호출 처리(재고 감소)](https://user-images.githubusercontent.com/84000922/124539148-dca11f80-de57-11eb-8d47-37e57c8a2a5f.png)

##### 예약서비스에서 렌트카를 예약 > 렌트카관리 서비스 Down 시 예약서비스의 예약도 실패

<img src="https://user-images.githubusercontent.com/84000922/124539200-f5a9d080-de57-11eb-92c7-3650c4d2afd0.png" alt="5  car 서비스 down" style="zoom:100%;" align=left />

##### ![5-1  car 서비스 down 후 예약 안됨](https://user-images.githubusercontent.com/84000922/124539268-13773580-de58-11eb-8be0-7d271f56978f.png)





# 운영

### Deploy

- 빌드하기 (패키징)

  ```
  cd rentcar
  cd car
  mvn package
  
  cd ../gateway
  mvn package
  
  cd ../mypage
  mvn package
  
  cd ../rental
  mvn package
  
  cd ../reservation
  mvn package
  ```

- namespace 등록 및 변경

  ```
  kubectl config set-context --current --namespace=rentcar  
  kubectl create ns rentcar
  ```

- yml 파일에 namespace 내용 추가 (모든 deployment, service 파일)

<img src="https://user-images.githubusercontent.com/84000922/124488991-74b4ef80-ddeb-11eb-813a-5eb43e29b903.png" alt="0  namespace" style="zoom:80%;" align=left />

- ACR 컨테이너이미지 빌드

  ```
  az acr build --registry user13skccacr --image user13skccacr.azurecr.io/car:v1 .
  az acr build --registry user13skccacr --image user13skccacr.azurecr.io/gateway:v1 .
  az acr build --registry user13skccacr --image user13skccacr.azurecr.io/mypage:v1 .
  az acr build --registry user13skccacr --image user13skccacr.azurecr.io/rental:v1 .
  az acr build --registry user13skccacr --image user13skccacr.azurecr.io/reservation:v1 .
  ```

- 배포 수행

  ```
  	cd car/kubernetes
  	kubectl apply -f deployment.yml
  	kubectl apply -f service.yaml
  	
  	cd ../../gateway/kubernetes
  	kubectl apply -f deployment.yml
  	kubectl apply -f service.yaml
  	
  	cd ../../mypage/kubernetes
  	kubectl apply -f deployment.yml
  	kubectl apply -f service.yaml
  		
  	cd ../../rental/kubernetes
  	kubectl apply -f deployment.yml
  	kubectl apply -f service.yaml
  		
  	cd ../../reservation/kubernetes
  	kubectl apply -f deployment.yml
  	kubectl apply -f service.yaml
  ```

- 배포결과 확인

  ```
  kubectl get all
  ```

  ![2  배포결과 확인](https://user-images.githubusercontent.com/84000922/124489264-c0679900-ddeb-11eb-87f0-2f868dba1ab3.png)

- Kafka 설치

  ```
  curl https://raw.githubusercontent.com/helm/helm/master/scripts/get > get_helm.sh
  chmod 700 get_helm.sh
  ./get_helm.sh
  
  kubectl --namespace kube-system create sa tiller 
  kubectl create clusterrolebinding tiller --clusterrole cluster-admin --serviceaccount=kube-system:tiller
  helm init --service-account tiller
  
  helm repo add incubator https://charts.helm.sh/incubator
  helm repo update
  
  kubectl create ns kafka
  helm install --name my-kafka --namespace kafka incubator/kafka
  
  kubectl get all -n kafka
  ```

  

### Circuit Breaker

서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Hystrix 옵션을 사용하여 구현
렌트카 예약시 렌트카 재고 확인 연결을 RESTful Request/Response 로 연동하여 구현
예약이 과도할 경우 CB 를 통하여 장애격리.

- Hystrix 를 설정: 요청처리 쓰레드에서 처리시간이 1000ms가 넘어서기 시작하면 CB 작동하도록 설정 (reservation > application.yml)

<img src="https://user-images.githubusercontent.com/84000922/124493583-c7dd7100-ddf0-11eb-9340-a5ad2f282a2f.png" alt="4  CB-hystrix 설정" style="zoom:100%;" align=left />

- 피호출 서비스(렌트카관리: car) 의 임의 부하 처리 - 800ms에서 증감 300ms 정도하여 800~1100 ms 사이에서 발생하도록 처리 CarController.java
  - req/res를 처리하는 피호출 function에 sleep 추가

<img src="https://user-images.githubusercontent.com/84000922/124494776-2bb46980-ddf2-11eb-9933-d280ee5ebcd0.png" alt="4  CB-임의 부하 처리" style="zoom: 67%;" align=left />

- siege 생성 (로드제너레이터 설치)

```
kubectl apply -f - <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: siege
  namespace: rentcar
spec:
  containers:
  - name: siege
    image: apexacme/siege-nginx
EOF
```

- req/res 호출하는 위치가 onPostPersist에 있어 실제로 Data가 발생하지 않으면 호출이 되지 않는 문제가 있어 siege를 실행하여 Data가 지속적으로 발생하게 처리 함

```
siege -c50 -t10S -v --content-type "application/json" 'http://20.194.116.172:8080/reservations POST {"carId":1,"userId":"AAA", "status":"Success"}'
```

![4  CB-부하처리 결과](https://user-images.githubusercontent.com/84000922/124547444-188fb100-de67-11eb-9f5e-a96c92b1b9f5.png)

- 예약 서비스가 죽지 않고 지속적으로 CB 에 의하여 회로가 열림과 닫힘이 벌어지면서 자원을 보호하고 있음



### Autoscale (HPA)

- 리소스에 대한 사용량 정의(reservation > deployment.yml)

<img src="https://user-images.githubusercontent.com/84000922/124502424-6cb27b00-ddfe-11eb-91a5-5ce39db039fb.png" alt="5  HPA-리소스 용량 정의" style="zoom:80%;" align=left />

- Autoscale 설정 (request값의 15%를 넘어서면 Replica를 5개까지 동적으로 확장)

```
kubectl autoscale deployment reservation --cpu-percent=15 --min=1 --max=5
```

- 부하발생 (100명 동시사용자, 10초간 부하)

```
siege -c100 -t10S -v --content-type "application/json" 'http://20.194.116.172:8080/reservations/1 PATCH {"carId":1,"userId":"CCC", "status":"Success"}'
```

- 모니터링 (부하증가로 스케일아웃되어지는 과정을 별도 창에서 모니터링)

```
watch kubectl get all
```

![5  HPA-테스트 전](https://user-images.githubusercontent.com/84000922/124504503-953c7400-de02-11eb-835d-fbda10025774.png)

![5  HPA-테스트 후](https://user-images.githubusercontent.com/84000922/124504517-9f5e7280-de02-11eb-891d-8fc7ae374eaa.png)



### Config Map

ConfigMap을 사용하여 변경가능성이 있는 설정을 관리

- 예약(reservation) 서비스에서 동기호출(Req/Res방식)로 연결되는 렌트카관리(Car) 서비스 url 정보 일부를 ConfigMap을 사용하여 구현

- 파일 수정

  - 예약 소스 변경 (reservation/src/main/java/rentcar/external/CarService.java)

  ![10  cm1](https://user-images.githubusercontent.com/84000922/124489681-3b30b400-ddec-11eb-9feb-faaba867885a.png)

- Yaml 파일 수정

  - application.yml (reservation/src/main/resources/application.yml)

  <img src="https://user-images.githubusercontent.com/84000922/124490818-87302880-dded-11eb-87ef-d8dacc2edefc.png" alt="10  cm2" align=left style="zoom:100%;" />

  - deploy yml (reservation/kubernetes/deployment.yml)

  <img src="https://user-images.githubusercontent.com/84000922/124491344-205f3f00-ddee-11eb-9339-2ae02b599259.png" alt="10  cm3" style="zoom:100%;" align=left />

- Config Map 생성 및 생성 확인

```
kubectl create configmap rentcar-cm --from-literal=url=car
kubectl get cm
```

```
kubectl get cm rentcar-cm -o yaml
```

![10-2  cm yaml 확인](https://user-images.githubusercontent.com/84000922/124492818-ea22bf00-ddef-11eb-9d0c-98e3d8248f60.png)

- Config Map 적용 전후 확인

```
kubectl get pod
```

<img src="https://user-images.githubusercontent.com/84000922/124490421-05d89600-dded-11eb-936c-faf917e9f62b.png" alt="10-1  cm 적용전" align=left  />

<img src="https://user-images.githubusercontent.com/84000922/124490456-112bc180-dded-11eb-9ede-3656fbd8db10.png" alt="10-3  cm 적용후" align=left  />



### Zero-Downtime deploy (Readiness Probe)

컨테이너의 상태를 주기적으로 체크하여 문제가 있는 컨테이너는 서비스에서 제외

- deployment.yml에 readinessProbe 설정 후 미설정 상태 테스트를 위해 주석처리

```
readinessProbe:
httpGet:
  path: '/reservations'
  port: 8080
initialDelaySeconds: 10
timeoutSeconds: 2
periodSeconds: 5
failureThreshold: 5
```

- deployment.yml에서 readinessProbe 미설정 상태(주석처리)로 siege 부하발생

```
siege -c2 -t180S -v --content-type "application/json" 'http://20.194.116.172:8080/reservations POST {"carId":1,"userId":"LLL", "status":"Success"}'
```

- hpa 설정에 의해 target 지수 초과하여 scale-out 진행
  - 정상 실행중인 reservation pod으로의 요청은 성공, 배포중인 pod으로의 요청은 실패

- 다시 readiness 정상 적용 후, Availability 100% 확인

![11 reaniness적용](https://user-images.githubusercontent.com/84000922/124551453-17fa1900-de6d-11eb-8df8-145630a036f5.png)



### Self-healing (Liveness Probe)

각 컨테이너의 상태를 주기적으로 체크(Health Check)해서 문제가 있는 컨테이너는 자동으로 재시작

- deployment.yml(rental 서비스)의 livenessProbe의 path 및 port를 잘못된 값으로 변경

<img src="https://user-images.githubusercontent.com/84000922/124544098-6bff0080-de61-11eb-9f75-c3d4bd4b48ab.png" alt="7  liveness-deployment설정" style="zoom:100%;" align=left />

- Pod 배포시 Retry 시도 확인

<img src="https://user-images.githubusercontent.com/84000922/124544519-360e4c00-de62-11eb-84c0-8191e7d4b903.png" alt="7  liveness-pod리스타트" style="zoom:150%;" />

