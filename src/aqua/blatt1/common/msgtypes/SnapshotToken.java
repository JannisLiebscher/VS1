package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class SnapshotToken implements Serializable {
	private final int snapshot;

	public SnapshotToken(int id) {
		this.snapshot = id;
	}

	public int getSnapshot() {
		return snapshot;
	}
}
