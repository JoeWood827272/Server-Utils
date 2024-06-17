package net.kyrptonaught.serverutils.switchableresourcepacks.status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PackStatus {
    private final HashMap<UUID, Status> packs = new HashMap<>();

    public void addPack(UUID packname, boolean tempPack) {
        packs.put(packname, new Status(tempPack, LoadingStatus.PENDING));
    }

    public Status getPack(UUID pack) {
        return packs.get(pack);
    }

    public Map<UUID, Status> getPacks() {
        return packs;
    }

    public void setPackLoadStatus(UUID packname, PackStatus.LoadingStatus status) {
        getPack(packname).setLoadingStatus(status);
    }

    public boolean isComplete(UUID pack) {
        return getPack(pack).getLoadingStatus() == PackStatus.LoadingStatus.FINISHED || getPack(pack).getLoadingStatus() == PackStatus.LoadingStatus.FAILED;
    }

    public boolean didFail(UUID pack) {
        return getPack(pack).getLoadingStatus() == PackStatus.LoadingStatus.FAILED;
    }

    public static class Status {
        private final boolean tempPack;
        private LoadingStatus loadingStatus;

        public Status(boolean tempPack, LoadingStatus loadingStatus) {
            this.tempPack = tempPack;
            this.loadingStatus = loadingStatus;
        }

        public void setLoadingStatus(LoadingStatus loadingStatus) {
            this.loadingStatus = loadingStatus;
        }

        public LoadingStatus getLoadingStatus() {
            return loadingStatus;
        }

        public boolean isTempPack() {
            return tempPack;
        }
    }

    public enum LoadingStatus {
        PENDING,
        STARTED,
        FAILED,
        FINISHED
    }
}
