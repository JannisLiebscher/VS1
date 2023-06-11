package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class NameResolutionResponse implements Serializable {
    private final int AquaPort;
    private final String RequestId;

    public NameResolutionResponse(int AquaPort,String RequestId) {
        this.AquaPort = AquaPort;
        this.RequestId = RequestId;
    }

    public int getAquaPort() {
        return AquaPort;
    }
    public String getRequestId() {
        return RequestId;
    }

}