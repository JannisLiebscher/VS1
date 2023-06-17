package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.security.Key;

public class KeyExchangeResponse implements Serializable {
    private Key key;
    public KeyExchangeResponse(Key key){
        this.key = key;
    }
    public Key getKey(){
        return key;
    }
}
