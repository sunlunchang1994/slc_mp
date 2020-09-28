package android.slc.mp.po;

import android.slc.mp.po.i.IAudioFolder;
import android.slc.mp.po.i.IAudioItem;
import android.slc.mp.po.i.IAudioResult;

import java.util.List;

/**
 * Created by slc
 */

public class AudioResult extends BaseResult<IAudioFolder, IAudioItem> implements IAudioResult {

    public AudioResult() {
        super();
    }

    public AudioResult(List<IAudioFolder> folders) {
        super(folders);
    }

}
