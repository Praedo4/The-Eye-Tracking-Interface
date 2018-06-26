package sample;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import sample.ET.ETCollection;
import sample.FS.FSCollection;


public class FeatureController {
    @FXML
            private TextField userIDTextBox, textIDTextBox;
    @FXML
            private TextField numberOfFixationsTextBox, meanFixationDurationTextBox, sdTextBox, skewnessTextBox;
    @FXML
            private TextField numberOfSaccadesTextBox, meanSaccadicDurationTextBox, meanSaccadicAmplitudeTextBox, meanSaccadicVelocityTextBox;
    @FXML
            private TextField regressionsTextBox,regressionProportionTextBox, lookaheadTextBox, lookaheadProportionTextBox;
    @FXML
            private TextField lineSaccadesTextBox, fixationsPerLineTextBox, perceptionPerLineTextBox, regressionsPerLineTextBox;
    @FXML
            private TextField meanPupilDilationTextBox, puiTextBox;
    String userID="_",textID="_";
    private FSCollection eventData;
    private ETCollection rawData;



    public void receiveParameters( int u,int t, FSCollection events, ETCollection samples){
        userID  = Integer.toString(u);
        textID = Integer.toString(t);
        eventData = new FSCollection();
        events.copyTo(eventData);
        rawData = new ETCollection();
        samples.copyTo(rawData);
    }


    public void initialize() {
        userIDTextBox.setText(userID);
        textIDTextBox.setText(textID);
        startButtonClick();
    }

    private String round3(double x){
        return Double.toString(Math.round(x*1000)/1000.0);
    }

    public void startButtonClick(){
        int n = eventData.size;

        // Fixations
        numberOfFixationsTextBox.setText(Integer.toString(n));
        double durations[] = new double[n];
        double totalFixationDuration = 0;
        for(int i = 0 ; i < n; i++) {
            durations[i] = eventData.events[i].duration / 1000000.0 ;
            totalFixationDuration += durations[i];
        }
        double meanDuration = totalFixationDuration / (1.0 * n);
        meanFixationDurationTextBox.setText(round3(meanDuration));

        double sum2 = 0, sum3 = 0;
        for(int i = 0 ; i < n; i ++){
            double deviation = durations[i] - meanDuration;
            sum2 += deviation * deviation;
            sum3 += deviation * deviation * deviation;

        }
        double variance = sum2 / (n - 1);
        double sd = Math.sqrt(variance);
        sdTextBox.setText(round3(sd));

        double moment3 = sum3 / (n - 1);
        double skewness = moment3 / Math.pow(variance,3/2);
        skewnessTextBox.setText(round3(skewness));


        // Saccades
        numberOfSaccadesTextBox.setText(Integer.toString(n-1));
        double totalAmplitude = 0;
        double totalVelocity = 0;
        int lineSaccades = 0, regressions = 0;
        double totalSaccadicDuration = 0;
        double perceptionTime = 0;
        int fixationCount = 0;
        for(int i = 0; i < n; i++){

            if(i > 0) {
                double x_dist = eventData.events[i].x - eventData.events[i - 1].x;
                double y_dist = eventData.events[i].y - eventData.events[i - 1].y;
                double duration = (eventData.events[i].start - eventData.events[i - 1].end) / 1000000.0;
                totalSaccadicDuration += duration;

                double amplitude = Math.sqrt(x_dist * x_dist + y_dist * y_dist);
                totalAmplitude += amplitude;

                double velocity = amplitude / duration;
                totalVelocity += velocity;

                double angle =  Math.abs(y_dist / x_dist);
                if (amplitude > 300 && angle < 0.08)
                    lineSaccades++;
                else if(y_dist < -20 || (y_dist < 0 && x_dist < 0))
                    regressions++;
            }

        }
        meanDuration = totalSaccadicDuration / (n - 1);
        meanSaccadicDurationTextBox.setText(round3(meanDuration));

        double meanAmplitude = totalAmplitude / (n - 1);
        meanSaccadicAmplitudeTextBox.setText(round3(meanAmplitude));

        double meanVelocity = totalVelocity / (n - 1);
        meanSaccadicVelocityTextBox.setText(round3(meanVelocity));

        lineSaccadesTextBox.setText(Integer.toString(lineSaccades));
        fixationsPerLineTextBox.setText(round3(n/(1.0*lineSaccades + 1)));
        perceptionPerLineTextBox.setText(round3(totalFixationDuration/(1.0*lineSaccades + 1)));

        regressionsTextBox.setText(Integer.toString(regressions));
        regressionProportionTextBox.setText(round3(regressions / (1.0*n - 1)));
        regressionsPerLineTextBox.setText(round3(regressions/(1.0*lineSaccades + 1)));

        //userIDTextBox.setText(userID);
        //textIDTextBox.setText(textID);

        // Pupillary
        n = rawData.size;
        int lastCount = n % 16, curCount = 16;
        int numberOfBins = (n + 15) / 16;
        if(lastCount == 0)
            lastCount = 16;
        double averageDilation[] = new double [numberOfBins];
        double curSum = 0;
        double totalDilationSum = 0;
        for(int i = 0; i < numberOfBins; i++) {
            curCount = (i == (numberOfBins - 1) ? lastCount : 16);
            curSum = 0;
            for (int j = 0; j < curCount; j++) {
                curSum += rawData.gazePoints[(i * 16) + j].pupil_diameter;
                totalDilationSum += rawData.gazePoints[(i * 16) + j].pupil_diameter;
            }
            averageDilation[i] = curSum / curCount;
        }

        double meanPupilDilation = totalDilationSum / n;
        meanPupilDilationTextBox.setText(round3(meanPupilDilation));

        double differenceSum = 0;
        for(int i = 1; i < numberOfBins; i++){
            differenceSum += Math.abs(averageDilation[i] - averageDilation[i-1]);
        }
        double pui = 1/((n - 16)*(1/60.0)) * differenceSum;
        puiTextBox.setText(round3(pui));

    }
}
