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
