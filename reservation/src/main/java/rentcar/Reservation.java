package rentcar;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;

@Entity
@Table(name="Reservation_table")
public class Reservation {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long reservationId;
    private Long carId;
    private String carName;
    private String userId;
    private String userName;
    private String reservedDate;
    private String status;

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


        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.

        // rentcar.external.Car car = new rentcar.external.Car();
        // mappings goes here

        // try{
        //     // mappings goes here
        //     boolean isUpdated = ReservationApplication.applicationContext.getBean(rentcar.external.CarService.class)
        //     .checkUpdateStock(getCarId(), getCarName(), getQty());

        //     if(isUpdated == false){
        //         throw new Exception("렌트카관리 서비스 재고 정보가 갱신되지 않음");
        //     }
        // }catch(java.net.ConnectException ce){
        //     throw new Exception("렌트카관리 서비스 연결 실패");
        // }catch(Exception e){
        //     throw new Exception("렌트카관리 서비스 처리 실패");
        // }


    }

    private Long getQty() {
        return null;
    }

    @PostUpdate
    public void onPostUpdate(){
        ReservationCancelled reservationCancelled = new ReservationCancelled();
        BeanUtils.copyProperties(this, reservationCancelled);
        reservationCancelled.publishAfterCommit();


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
    public String getCarName() {
        return carName;
    }

    public void setCarName(String carName) {
        this.carName = carName;
    }
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
    public String getReservedDate() {
        return reservedDate;
    }

    public void setReservedDate(String reservedDate) {
        this.reservedDate = reservedDate;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }




}
