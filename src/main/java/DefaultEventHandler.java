public class DefaultEventHandler implements EventHandler {

    public void onRead(String message){
        System.out.println("println from DefaultEventHandler.onRead------message:"+message);
    }

    public String onWrite(String message){
        return "hello nico from EventHandle.onWrite and "+message;
    }
}
