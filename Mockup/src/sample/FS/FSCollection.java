package sample.FS;

public class FSCollection {
    public FSEvent events[];
    public int size;


    public void copyTo(FSCollection dest){
        dest.size = size;
        dest.events = new FSEvent[size];
        for(int i = 0; i < size; i ++){
            events[i].copyTo(dest.events[i]);

        }
    }

    public void initialize(FSEvent data[]){
        size = data.length;
        events = new FSEvent[size];
        for(int i = 0; i < size; i ++){
            events[i] = new FSEvent();
            data[i].copyTo(events[i]);

        }
    }
}
