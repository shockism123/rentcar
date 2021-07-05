package rentcar;

public class CarReserved extends AbstractEvent {

    private Long reserationId;
    private Long carId;
    private String carName;
    private String userId;
    private String userName;
    private String reservedDate;
    private String status;

    public Long getReserationId() {
        return reserationId;
    }

    public void setReserationId(Long reserationId) {
        this.reserationId = reserationId;
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