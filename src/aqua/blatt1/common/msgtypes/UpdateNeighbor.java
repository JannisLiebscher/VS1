package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

public final class UpdateNeighbor implements Serializable {
    private final int side;
    private final InetSocketAddress neigbor;

    public UpdateNeighbor(int side, InetSocketAddress neighbor) {
        this.side = side;
        this.neigbor = neighbor;
    }

    public int getSide() {
        return side;
    }
    public InetSocketAddress getNeigbor() { return neigbor;}

}
