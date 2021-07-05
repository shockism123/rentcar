
package rentcar.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

//@FeignClient(name="car", url="http://${api.url.rentcar}:8080", fallback = CarServiceFallback.class)
//@FeignClient(name="car", url="http://car:8081", fallback = CarServiceFallback.class)
@FeignClient(name="car", url="http://car:8081")
public interface CarService {

    @RequestMapping(method= RequestMethod.GET, path="/cars/checkUpdateStock")
    public boolean checkUpdateStock(@RequestParam("carId") Long carId);

}