package rentcar;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;

@Entity
@Table(name="Car_table")
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long carId;
    private String carName;
    private Long qty;

    @PostPersist
    public void onPostPersist(){
        CarRegistered carRegistered = new CarRegistered();
        BeanUtils.copyProperties(this, carRegistered);
        carRegistered.publishAfterCommit();


    }

    @PostUpdate
    public void onPostUpdate(){
        StockUpdated stockUpdated = new StockUpdated();
        BeanUtils.copyProperties(this, stockUpdated);
        stockUpdated.publishAfterCommit();


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
    public Long getQty() {
        return qty;
    }

    public void setQty(Long qty) {
        this.qty = qty;
    }




}
