package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class LocationUpdate implements Serializable {

    private final String RequestId;

    public LocationUpdate(String RequestId) {
        this.RequestId = RequestId;
    }

    public String getRequestId() {
        return RequestId;
    }

}