package rentcar;

import rentcar.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MypageViewHandler {


    @Autowired
    private MypageRepository mypageRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenCarReserved_then_CREATE_1 (@Payload CarReserved carReserved) {
        try {

            if (!carReserved.validate()) return;

            // view 객체 생성
            Mypage mypage = new Mypage();
            // view 객체에 이벤트의 Value 를 set 함
            mypage.setReservationId(carReserved.getReserationId());
            mypage.setCarId(carReserved.getCarId());
            mypage.setUserId(carReserved.getUserId());
            mypage.setStatus(carReserved.getStatus());
            // view 레파지 토리에 save
            mypageRepository.save(mypage);
        
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whenReservationCancelled_then_UPDATE_1(@Payload ReservationCancelled reservationCancelled) {
        try {
            if (!reservationCancelled.validate()) return;
                // view 객체 조회
            Optional<Mypage> mypageOptional = mypageRepository.findByReservationId(reservationCancelled.getReservationId());
            if( mypageOptional.isPresent()) {
                Mypage mypage = mypageOptional.get();
                // view 객체에 이벤트의 eventDirectValue 를 set 함
                    mypage.setStatus(reservationCancelled.getStatus());
                // view 레파지 토리에 save
                mypageRepository.save(mypage);
            }
            
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenUseCompleted_then_UPDATE_2(@Payload UseCompleted useCompleted) {
        try {
            if (!useCompleted.validate()) return;
                // view 객체 조회
            Optional<Mypage> mypageOptional = mypageRepository.findByReservationId(useCompleted.getReservationId());
            if( mypageOptional.isPresent()) {
                Mypage mypage = mypageOptional.get();
                // view 객체에 이벤트의 eventDirectValue 를 set 함
                    mypage.setStatus(useCompleted.getStatus());
                // view 레파지 토리에 save
                mypageRepository.save(mypage);
            }
            
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whenCarRegistered_then_DELETE_1(@Payload CarRegistered carRegistered) {
        try {
            if (!carRegistered.validate()) return;
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}