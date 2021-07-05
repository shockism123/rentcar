package rentcar;

import rentcar.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired CarRepository carRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverAllocationCancelled_UpdateStock(@Payload AllocationCancelled allocationCancelled){

        if(!allocationCancelled.validate()) return;

        System.out.println("\n\n##### listener UpdateStock : " + allocationCancelled.toJson() + "\n\n");

        // Sample Logic //
        Car car = new Car();
        car.setQty(car.getQty()+1);
        carRepository.save(car);
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCarAllocated_UpdateStock(@Payload CarAllocated carAllocated){

        if(!carAllocated.validate()) return;

        System.out.println("\n\n##### listener UpdateStock : " + carAllocated.toJson() + "\n\n");

        // Sample Logic //
        Car car = new Car();
        car.setQty(car.getQty()-1);
        carRepository.save(car);
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverUseCompleted_UpdateStock(@Payload UseCompleted useCompleted){

        if(!useCompleted.validate()) return;

        System.out.println("\n\n##### listener UpdateStock : " + useCompleted.toJson() + "\n\n");

        // Sample Logic //
        Car car = new Car();
        car.setQty(car.getQty()+1);
        carRepository.save(car);
            
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
