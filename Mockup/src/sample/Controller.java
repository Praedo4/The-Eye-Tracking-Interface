package sample;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;

import javafx.scene.control.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import org.rosuda.JRI.*;
import sample.ET.ETCollection;
import sample.ET.ETReader;
import sample.ET.GazePoint;
import sample.FS.FSCollection;
import sample.FS.FSEvent;
import sample.FS.FSReader;


import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map;
import java.util.HashMap;
import java.io.*;

public class Controller {

    @FXML private TextField label;
    @FXML private Canvas surface;
    @FXML private RadioButton eventsRadioButton;
    @FXML private RadioButton gazePointsRadioButton;
    @FXML private RadioButton tetRadioButton;
    @FXML private RadioButton dispersionRadioButton;
    @FXML private RadioButton durationRadioButton;
    @FXML private CheckBox cleanCheckBox;
    @FXML private ListView<String> userList;
    @FXML private ListView<Integer> textList;
    @FXML private Label visualizationLabel;
    @FXML private TextField dispersionTextBox;
    @FXML private TextField durationTextBox;
    private GraphicsContext gc;
    private Random randomNum;
    private Timer tm;
    Rengine re;
    //private FileReader fr;
    //private BufferedReader br;
    int index = 0;
    String fileName = "data/raw_data/p01_ET_samples.txt";
    private ETCollection rawData;
    private FSCollection eventData;
    int mode;
    boolean clean, durationDisplay = false;
    final int RAW = 0, EVENTS = 1, TET = 2;
    int selectedTextID = -1;
    long durationLeft = 0;
    int idtDispersion, idtDuration;
    boolean isIDT = false;
    Map<Integer, String> imageFilenames;

    public void println(String text){
        System.out.println(text);
    }

    public void clearScreen(){
        gc.clearRect(0,0,surface.getWidth(),surface.getHeight());

        Image textImage = getImage(selectedTextID);
        gc.drawImage(textImage,0,0);
    }

    public void readImages(){
        imageFilenames = new HashMap<Integer, String>();
       try {
           FileReader fileReader = new FileReader("data/screen-shots/table.txt");
           BufferedReader bufferedReader = new BufferedReader(fileReader);
           String line;
           String splitLine[];
           while((line = bufferedReader.readLine()) != null){
               splitLine = line.split("\\s");
               imageFilenames.put(Integer.parseInt(splitLine[1]),splitLine[0]);
           }
       }
       catch (FileNotFoundException ex){
           println("Error reading the image");
       }
       catch (IOException ex){
           println("Error reading the image");
       }
    }

    public Image getImage(int textID){

        File imageFile = new File("data/screen-shots/" + imageFilenames.get(textID));
        Image img = new Image(imageFile.toURI().toString());
        return  img;

    }

    private boolean withinScreen(double x, double y){
        if( x < 0 || y < 0 || x > surface.getWidth() || y > surface.getHeight())
            return false;
        return  true;
    }

    public void initialize() {
        label.setText(fileName);
        gc = surface.getGraphicsContext2D();
        randomNum = new Random();
        changeDataMode();
        setClean();
        readImages();
        setIdtDispersion();
        setIdtDuration();

        ListView<String> list = new ListView<String>();
        File folder = new File("data/raw_data");
        File[] listOfFiles = folder.listFiles();
        String[] fileNames = new String[listOfFiles.length];
        for(int i = 0; i < listOfFiles.length; i++)
            fileNames[i] = listOfFiles[i].getName().substring(1,3);
        ObservableList<String> items = FXCollections.observableArrayList (fileNames);
        userList.setItems(items);
        ObservableList<Integer> textIDs = FXCollections.observableArrayList(11299,13165,5938,11143,322,10879,4584,4504,6366,4701,6474,4094,7784,6046,2725,3775,9977,3819);
        textList.setItems(textIDs);

        userList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {

            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                // Your action here
                if(mode == RAW) {
                    fileName = "data/split_raw_data/p" + newValue + "_" + textList.getSelectionModel().getSelectedItem() + "_ET.txt";
                    label.setText(fileName);
                }
                else if (mode == EVENTS){
                    fileName = "data/split_aggregated_data/p" + newValue + "_" + textList.getSelectionModel().getSelectedItem() + "_FS.txt";
                    label.setText(fileName);
                }
            }
        });

        textList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Integer>() {

            @Override
            public void changed(ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue) {
                // Your action here
                selectedTextID = newValue;
                if(mode == RAW) {
                    fileName = "data/split_raw_data/p" + userList.getSelectionModel().getSelectedItem() + "_" + newValue + "_ET.txt";
                    label.setText(fileName);
                }
                else if (mode == EVENTS){
                    fileName = "data/split_aggregated_data/p" + userList.getSelectionModel().getSelectedItem() + "_" + newValue + "_FS.txt";
                    label.setText(fileName);
                }
            }
        });

        String args[] = null;
        re=new Rengine(args, false, new TextConsole());
        re.eval("require(emov)");
        durationRadioButton.setSelected(true);
        userList.getSelectionModel().select(0);
        textList.getSelectionModel().select(0);
        durationDisplay = true;


    }

    public void startButtonClick(){
        changeDataMode();
        setClean();
        isIDT = false;
        startVisualization();
    }

    public void startVisualization(){
        if(tm != null){
            tm.cancel();
            tm.purge();
        }
        tm = new Timer();
        index = 0;
        clearScreen();
        if (isIDT){
            TimerTask task = new TimerTask() {
                public void run() {
                    if(durationLeft != 0) {
                        durationLeft--;
                        return;
                    }
                    if(index >= eventData.size - 6){
                        tm.cancel();
                        tm.purge();
                        println("The end");
                        return;
                    }
                    FSEvent event = eventData.events[index];
                    if(clean && !withinScreen(event.x,event.y)){
                        index++;
                        return;
                    }
                    if(event.type == event.FIXATION){
                        //println(( Long.toString(event.duration)));
                        double disp = 1 + event.duration / 20000.0;
                        durationLeft = event.duration / 40000;
                        if(index != 0){
                            FSEvent prevEvent = eventData.events[index-1];
                            if(!clean || (withinScreen(event.x,event.y) && withinScreen(prevEvent.x, prevEvent.y)))
                                gc.strokeLine( prevEvent.x, prevEvent.y, event.x, event.y);

                        }
                        if(durationDisplay) {
                            gc.strokeOval(event.x - disp / 2, event.y - disp / 2, disp, disp);
                        }
                        else{
                            gc.strokeOval(event.x - event.x2/2, event.y - event.y2/2, event.x2, event.y2);
                        }
                    }
                    else if(event.type == event.SACCADE){
                        if(clean && !withinScreen(event.x2,event.y2)){
                            index++;
                            return;
                        }
                        gc.strokeLine(event.x, event.y, event.x2, event.y2);
                    }
                    else{
                        println("Error reading event");
                    }
                    index++;
                }

            };
            tm.schedule(task,20,20);
        }
        else if(mode == RAW) {
            ETReader etReader = new ETReader();
            rawData = etReader.readETCollection(fileName);
            TimerTask task = new TimerTask() {
                public void run() {
                    if(index >= rawData.size){
                        tm.cancel();
                        tm.purge();
                        println("The end");
                        return;
                    }
                    int w = 5, h = 5;
                    GazePoint pt = rawData.gazePoints[index];
                    double x = pt.x;
                    double y = pt.y;
                    index++;
                    if(clean && !withinScreen(x,y))
                        return;
                    gc.strokeOval(x, y, w, h);
                }

            };
            tm.schedule(task,20,20);
        }
        else if (mode == EVENTS){
            FSReader fsReader = new FSReader();
            eventData = fsReader.readFSCollection(fileName);
            TimerTask task = new TimerTask() {
                public void run() {
                    if(durationLeft != 0) {
                        durationLeft--;
                        return;
                    }
                    if(index >= eventData.size - 6){
                        tm.cancel();
                        tm.purge();
                        println("The end");
                        return;
                    }
                    FSEvent event = eventData.events[index];
                    if(clean && !withinScreen(event.x,event.y)){
                        index++;
                        return;
                    }
                    if(event.type == event.FIXATION){
                        //println(( Long.toString(event.duration)));
                        double disp = 1 + event.duration / 20000.0;
                        durationLeft = event.duration / 40000;
                        if(index != 0){
                            FSEvent prevEvent = eventData.events[index-1];
                            if(!clean || (withinScreen(event.x,event.y) && withinScreen(prevEvent.x, prevEvent.y)))
                                gc.strokeLine( prevEvent.x, prevEvent.y, event.x, event.y);

                        }
                        if(durationDisplay) {
                            gc.strokeOval(event.x - disp / 2, event.y - disp / 2, disp, disp);
                        }
                        else{
                            gc.strokeOval(event.x - event.x2/2, event.y - event.y2/2, event.x2, event.y2);
                        }
                    }
                    else if(event.type == event.SACCADE){
                        if(clean && !withinScreen(event.x2,event.y2)){
                            index++;
                            return;
                        }
                        gc.strokeLine(event.x, event.y, event.x2, event.y2);
                    }
                    else{
                        println("Error reading event");
                    }
                    index++;
                }

            };
            tm.schedule(task,20,20);


        }

        else if(mode == TET){

            ETReader etReader =  new ETReader();
            rawData = etReader.readTETCollection(fileName);
            TimerTask task = new TimerTask() {
                public void run() {
                    if(index >= rawData.size){
                        tm.cancel();
                        tm.purge();
                        println("The end");
                        return;
                    }
                    int w = 5, h = 5;
                    GazePoint pt = rawData.gazePoints[index];
                    double x = pt.x;
                    double y = pt.y;
                    index++;
                    if(clean && !withinScreen(x,y))
                        return;
                    gc.strokeOval(x, y, w, h);
                }

            };
            tm.schedule(task,20,20);
        }
    }

    public void runEventDetection(){
        // Create lists of timestamps, xs and ys
        //if(mode != RAW){
            gazePointsRadioButton.setSelected(true);
            changeDataMode();
            setClean();
            ETReader etReader = new ETReader();
            rawData = etReader.readETCollection(fileName);
        //}

        int n = rawData.size;
        int [] timestamps = new int [n];
        long timeStampStart;
        double xs[] = new double[n],ys[] = new double[n];
        timeStampStart = rawData.gazePoints[0].timestamp;
        for(int i = 0 ; i < n-6; i++){
            GazePoint pt = rawData.gazePoints[i];
            timestamps[i] = (int) (pt.timestamp - timeStampStart);
            xs[i] = pt.x;
            ys[i] = pt.y;
        }

        REXP rResult;
        // Pass the data to R
        if(re.assign("t",timestamps) && re.assign("x",xs) && re.assign("y", ys)){
            // Run IDT
            String callIDT = "emov.idt(t,x,y," + idtDispersion + "," + idtDuration + ")";
            rResult = re.eval(callIDT);
            // Get the data back from R
            RList l = rResult.asList();
            int starts[] = l.at(0).asIntArray();
            int ends[] = l.at(1).asIntArray();
            int durs[] = l.at(2).asIntArray();
            double out_xs[] = l.at(3).asDoubleArray();
            double out_ys[] = l.at(4).asDoubleArray();
            // Parse the data
            int numberOfFixations = starts.length;
            FSEvent[] fsData = new FSEvent[numberOfFixations];
            for(int i = 0; i < numberOfFixations; i++){
                System.out.printf("Fixation #%d: starts at %d ends at %d (duration: %d) at position (%f, %f)\n",i+1,starts[i],ends[i],durs[i],out_xs[i],out_ys[i]);
                eventData = new FSCollection();
                fsData[i] = new FSEvent(out_xs[i],out_ys[i],1,1,timeStampStart + starts[i], timeStampStart + ends[i], durs[i], true);
            }
            eventData.initialize(fsData);
            // Visualize the data
            isIDT = true;
            //showEventVisualizationControls(1);
            startVisualization();
        }
        else{
            println("Failed to assign data to R");
        }

    }

    private void showEventVisualizationControls(int op){
        dispersionRadioButton.setOpacity(op);
        durationRadioButton.setOpacity(op);
        cleanCheckBox.setOpacity(op);
        visualizationLabel.setOpacity(op);

    }

    public void changeDataMode(){
        if(gazePointsRadioButton.isSelected()) {
            mode = RAW;
            fileName = "data/split_raw_data/p" + userList.getSelectionModel().getSelectedItem()+ "_" + textList.getSelectionModel().getSelectedItem() + "_ET.txt";
            label.setText(fileName);
           //showEventVisualizationControls(0);

        }
        else if(eventsRadioButton.isSelected()) {
            mode = EVENTS;
            fileName = "data/split_aggregated_data/p" + userList.getSelectionModel().getSelectedItem() + "_" + textList.getSelectionModel().getSelectedItem() + "_FS.txt";
            label.setText(fileName);
           // showEventVisualizationControls(1);
        }
        else if(tetRadioButton.isSelected()) {
            mode = TET;
            fileName = "data/tet/TET_raw_" + userList.getSelectionModel().getSelectedItem() + ".txt";
            label.setText(fileName);
           // showEventVisualizationControls(0);
        }
    }

    public void changeEventVisualizationMode(){
        if(durationRadioButton.isSelected()) {
            durationDisplay = true;
        }
        else if(dispersionRadioButton.isSelected()){
            durationDisplay = false;
        }
    }

    public void setClean(){
        clean = cleanCheckBox.isSelected();
    }

    public void setIdtDispersion(){
        try{
            idtDispersion = Integer.parseInt(dispersionTextBox.getText());
        }
        catch (NumberFormatException ex){
            dispersionTextBox.setText("Enter integer");
        }
    }

    public void setIdtDuration(){
        try{
            idtDuration = Integer.parseInt(durationTextBox.getText());
        }
        catch (NumberFormatException ex){
            durationTextBox.setText("Enter integer");
        }
    }

    public void chooseFile(){
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setInitialDirectory(new File("data/.").getAbsoluteFile());
        File file = fileChooser.showOpenDialog(surface.getScene().getWindow());
        try{
            if(file == null)
                return;
            fileName = file.getCanonicalPath();
            if(tm!=null) {
                tm.cancel();
                tm.purge();
            }
            selectedTextID = -1;
            initialize();
        }
        catch(IOException ex){
            println("File not found");
        }
    }

    public void stop(){
        if(tm!=null) {
            tm.cancel();
            tm.purge();
        }
    }



    //EventHandler<ActionEvent> buttonHandler = new EventHandler<ActionEvent>() {
    //    @FXML
    //    public void handle(ActionEvent event) {
    //        statusLabel.setText("Accepted");
    //        event.consume();
    //    }
    //};

}
