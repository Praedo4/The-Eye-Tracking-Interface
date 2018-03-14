package sample;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;

import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
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
    @FXML private CheckBox cleanCheckBox;
    @FXML private ListView<String> userList;
    @FXML private ListView<Integer> textList;
    private GraphicsContext gc;
    private Random randomNum;
    private Timer tm;
    //private FileReader fr;
    //private BufferedReader br;
    int index = 0;
    String fileName = "data/raw_data/p01_ET_samples.txt";
    private ETCollection rawData;
    private FSCollection eventData;
    int mode;
    boolean clean;
    final int RAW = 0, EVENTS = 1;
    int selectedTextID = -1;
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
        changeMode();
        setClean();
        readImages();

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
                    fileName = "data/raw_data/p" + newValue + "_ET_samples.txt";
                    label.setText(fileName);
                }
                else if (mode == EVENTS){
                    fileName = "data/events_data/p" + newValue + ".txt";
                    label.setText(fileName);
                }
            }
        });

        textList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Integer>() {

            @Override
            public void changed(ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue) {
                // Your action here
                selectedTextID = newValue;
            }
        });

    }

    public void startButtonClick(){
        clearScreen();

        if(tm != null){
            tm.cancel();
            tm.purge();
        }
        tm = new Timer();
        index = 0;
        changeMode();
        setClean();
        if(mode == RAW) {
            ETReader etReader;
            etReader = new ETReader();
            rawData = etReader.readETCollection(fileName, selectedTextID);
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
            FSReader fsReader;
            fsReader = new FSReader();
            eventData = fsReader.readFSCollection(fileName, imageFilenames.get(selectedTextID));
            TimerTask task = new TimerTask() {
                public void run() {
                    if(index >= eventData.size){
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
                        gc.strokeOval(event.x, event.y, event.x2, event.y2);
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
    }

    public void changeMode(){
        if(gazePointsRadioButton.isSelected()) {
            mode = RAW;
            fileName = "data/raw_data/p" + userList.getSelectionModel().getSelectedItem() + "_ET_samples.txt";
            label.setText(fileName);
        }
        if(eventsRadioButton.isSelected()) {
            mode = EVENTS;
            fileName = "data/events_data/p" + userList.getSelectionModel().getSelectedItem() + ".txt";
            label.setText(fileName);
        }
    }

    public void setClean(){
        clean = cleanCheckBox.isSelected();
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
