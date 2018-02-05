public class Reply {

    private boolean writeBack;
    private byte[] writeBytes;

    public Reply(boolean writeBack, byte[] writeBytes){
        this.writeBack=writeBack;
        if(writeBytes!=null)
            this.writeBytes=writeBytes;
    }

    public boolean isWriteBack(){
        return writeBack;
    }

    public byte[] getWriteBytes(){
        return writeBytes;
    }

}
