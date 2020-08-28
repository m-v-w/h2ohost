package certisandy.h2ohost.controllers;

import certisandy.h2ohost.PredictionService;
import hex.genmodel.easy.exception.PredictException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class ApiController {
    @Autowired
    private PredictionService predictionService;
    @CrossOrigin()
    @RequestMapping("/predict")
    public Map<String,Double> predict(@RequestParam("model") String modelName,
                                      @RequestParam(value="calibrated", required = false) boolean calibrated,
                                      @RequestBody Map<String,Object> map) throws PredictException {
        return predictionService.predict(modelName,map,calibrated);
    }
    @CrossOrigin()
    @RequestMapping("/api/Service/Health")
    public String health() throws Exception {
        if(!predictionService.isLoaded())
            throw new Exception("Not loaded");
        return "Okay";
    }
}
