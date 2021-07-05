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
