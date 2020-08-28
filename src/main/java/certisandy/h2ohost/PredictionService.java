package certisandy.h2ohost;

import hex.ModelCategory;
import hex.genmodel.MojoModel;
import hex.genmodel.easy.EasyPredictModelWrapper;
import hex.genmodel.easy.RowData;
import hex.genmodel.easy.exception.PredictException;
import hex.genmodel.easy.prediction.AbstractPrediction;
import hex.genmodel.easy.prediction.BinomialModelPrediction;
import hex.genmodel.easy.prediction.MultinomialModelPrediction;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class PredictionService {
    public static final String WEIGHT_FEATURE = "Weight";
    private static final Logger logger = Logger.getLogger("PredictionService");
    private final Map<String,EasyPredictModelWrapper> models = new HashMap<>();
    @PostConstruct
    public void init() throws IOException {
        File f = new File("models/");
        for(File zip : f.listFiles((dir, name) -> name.endsWith(".zip")))
        {
            String name = zip.getName();
            logger.info("loading "+name);
            int idx = name.lastIndexOf('.');
            load(name.substring(0,idx),zip.toString());
        }
    }
    public boolean isLoaded() {
        return models.size() > 0;
    }
    private void load(String name,String file) throws IOException {
        EasyPredictModelWrapper.Config config = new EasyPredictModelWrapper.Config()
                .setModel(MojoModel.load(file));
        this.models.put(name,new EasyPredictModelWrapper(config));
    }
    public static Object fixMissing(String col)
    {
        switch (col)
        {
            case "PeerMidDiff":
            case "PeerMinDiff":
            case "PeerNoArbAvgDiff":
            case "PeerNoArbMinDiff":
            case "PeerNoArbVolDiff":
            case "PeerNoArbVolPrice":
            case "PeerCurvature":
            case "PeerExclMinDiff":
                return 0.0;
            case "LastSpreadChangeHour":
                return 24.0;
            case "ItmProbChange":
                return 1.0;
            case WEIGHT_FEATURE:
                return 1.0;
            case "EurexPeerBidDiff":
            case "EurexPeerAskDiff":
                return null;
            case "CertiMarket":
                return "DE";
            default:
                throw new IllegalArgumentException("missing required value on "+col);
        }
    }
    public Map<String,Double> predict(String modelName,Map<String,Object> input, boolean calibrated) throws PredictException {
        EasyPredictModelWrapper model = models.get(modelName);
        if(model == null)
            throw new IllegalArgumentException("Unknown model "+modelName);
        RowData row = new RowData();
        for(String col : model.m.features())
        {
            if(col.equals(model.m.getResponseName()))
                continue;
            Object v = input.get(col);
            if(v == null)
            {
                v = fixMissing(col);
            }
            if(v != null) {
                if (v instanceof Integer)
                    v = ((Integer) v).doubleValue();
                row.put(col, v);
            }
        }
        String[] names = model.getResponseDomainValues();
        Map<String,Double> result = new HashMap<>();
        double[] classProbabilities;
        if(model.m.getModelCategory() == ModelCategory.Multinomial) {
            classProbabilities = model.predictMultinomial(row).classProbabilities;
        }
        else if(model.m.getModelCategory() == ModelCategory.Binomial) {
            if(calibrated)
                classProbabilities = model.predictBinomial(row).calibratedClassProbabilities;
            else
                classProbabilities = model.predictBinomial(row).classProbabilities;
        }
        else
            throw new IllegalStateException("unsupported model category "+model.m.getModelCategory());
        for(int i=0; i<classProbabilities.length; i++)
        {
            result.put(names[i],classProbabilities[i]);
        }
        return result;
    }
    @PreDestroy
    public void destroy() {

    }
}
