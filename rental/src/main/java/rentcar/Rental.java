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
