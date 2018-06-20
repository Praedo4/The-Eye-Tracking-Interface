package sample;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import sample.FS.FSCollection;


public class FeatureController {
    @FXML
    private TextField userIDTextBox, textIDTextBox;
    @FXML
    private TextField numberOfFixationsTextBox, meanFixationDurationTextBox, sdTextBox, skewnessTextBox;
    @FXML
    private TextField numberOfSaccadesTextBox, meanSaccadicDurationTextBox, meanSaccadicAmplitudeTextBox, meanSaccadicVelocityTextBox;

    @FXML
    private TextField lineSaccadesTextBox, fixationsPerLineTextBox, perceptionPerLineTextBox, regressionsPerLineTextBox;
    String userID="_",textID="_";
    private FSCollection eventData;



    public void receiveParameters( int u,int t, FSCollection events){
        userID  = Integer.toString(u);
        textID = Integer.toString(t);
        eventData = new FSCollection();
        events.copyTo(eventData);
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
        int lineSaccades = 0;
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

                if (amplitude > 300 && y_dist / x_dist < 0.03)
                    lineSaccades++;
            }

        }
        meanDuration = totalSaccadicDuration / (n - 1);
        meanSaccadicDurationTextBox.setText(String.format("%f",meanDuration));

        double meanAmplitude = totalAmplitude / (n - 1);
        meanSaccadicAmplitudeTextBox.setText(round3(meanAmplitude));

        double meanVelocity = totalVelocity / (n - 1);
        meanSaccadicVelocityTextBox.setText(round3(meanVelocity));

        lineSaccadesTextBox.setText(Integer.toString(lineSaccades));
        fixationsPerLineTextBox.setText(round3(n/(1.0*lineSaccades)));
        perceptionPerLineTextBox.setText(round3(totalFixationDuration/(1.0*lineSaccades)));
        //userIDTextBox.setText(userID);
        //textIDTextBox.setText(textID);
    }
}
