package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class NameResolutionRequest implements Serializable {
    private final String TankId;
    private final String RequestId;

    public NameResolutionRequest(String TankId,String RequestId) {
        this.TankId = TankId;
        this.RequestId = RequestId;
    }

    public String getTankId() {
        return TankId;
    }
    public String getRequestId() {
        return RequestId;
    }

}