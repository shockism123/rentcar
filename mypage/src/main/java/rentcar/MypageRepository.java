package rentcar;

import org.springframework.data.repository.CrudRepository;
import java.util.Optional;

public interface MypageRepository extends CrudRepository<Mypage, Long> {

    Optional<Mypage> findByReservationId(Long reservationId);


}