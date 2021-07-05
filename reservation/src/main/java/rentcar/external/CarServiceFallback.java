
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