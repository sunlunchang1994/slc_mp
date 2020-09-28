package android.slc.mp.ui.page;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.slc.medialoader.MediaLoader;
import android.slc.medialoader.callback.OnPhotoLoaderBaseCallBack;
import android.slc.medialoader.utils.MediaLoaderFileUtils;
import android.slc.medialoader.utils.MediaLoaderUriUtils;
import android.slc.mp.SlcMp;
import android.slc.mp.po.AddPhotoItem;
import android.slc.mp.po.PhotoFolder;
import android.slc.mp.po.PhotoItem;
import android.slc.mp.po.PhotoResult;
import android.slc.mp.po.SelectEvent;
import android.slc.mp.po.i.IPhotoFolder;
import android.slc.mp.po.i.IPhotoItem;
import android.slc.mp.po.i.IPhotoResult;
import android.slc.mp.ui.SlcIMpDelegate;
import android.slc.mp.utils.SlcMpFilePickerUtils;
import android.slc.mp.utils.SlcMpMediaBrowseUtils;
import android.util.Log;

import androidx.activity.result.ActivityResultCaller;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.content.Loader;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static android.provider.BaseColumns._ID;
import static android.provider.MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME;
import static android.provider.MediaStore.Images.ImageColumns.BUCKET_ID;
import static android.provider.MediaStore.MediaColumns.DATA;
import static android.provider.MediaStore.MediaColumns.DATE_MODIFIED;
import static android.provider.MediaStore.MediaColumns.DISPLAY_NAME;
import static android.provider.MediaStore.MediaColumns.SIZE;

public class SlcMpPagerPhotoDelegateImp extends SlcMpPagerBaseDelegateImp<IPhotoResult, IPhotoFolder, IPhotoItem> {
    private List<IPhotoItem> mHeadItem = new ArrayList<>();
    private WeakReference<OnResultListener<List<IPhotoItem>>> resultListenerWeakReference;

    public SlcMpPagerPhotoDelegateImp(SlcIMpDelegate mediaPickerDelegate) {
        super(SlcMp.MEDIA_TYPE_PHOTO, mediaPickerDelegate);
        mHeadItem.add(new AddPhotoItem());
    }

    @Override
    public void loader(FragmentActivity fragmentActivity, final OnResultListener<List<IPhotoItem>> loaderCallBack) {
        resultListenerWeakReference = new WeakReference<>(loaderCallBack);
        MediaLoader.getLoader().loadPhotos(fragmentActivity,
                new OnPhotoLoaderBaseCallBack<IPhotoResult>(mMediaPickerDelegate.getFilePropertyWithMediaType(getMediaType())) {
                    @Override
                    public void onResult(IPhotoResult result) {
                        mResult = result;
                        loaderCallBack.onLoadResult(getCurrentItemList());
                    }

                    @Override
                    public void onLoadFinish(Loader<Cursor> loader, Cursor data) {
                        Log.i("onLoadFinish", "00");
                        List<IPhotoFolder> folders = new ArrayList<>();
                        if (data != null) {
                            IPhotoFolder folder, allItemFolder = new PhotoFolder();
                            PhotoItem item;
                            while (data.moveToNext()) {
                                String folderId = data.getString(data.getColumnIndexOrThrow(BUCKET_ID));
                                String folderName = data.getString(data.getColumnIndexOrThrow(BUCKET_DISPLAY_NAME));
                                long imageId = data.getLong(data.getColumnIndexOrThrow(_ID));
                                String name = data.getString(data.getColumnIndexOrThrow(DISPLAY_NAME));
                                long size = data.getLong(data.getColumnIndexOrThrow(SIZE));
                                String path = data.getString(data.getColumnIndexOrThrow(DATA));
                                long modified = data.getLong(data.getColumnIndexOrThrow(DATE_MODIFIED));
                                int width = data.getInt(data.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH));
                                int height = data.getInt(data.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT));
                                item = new PhotoItem(imageId, name, path, size, modified, width, height);
                                item.setUri(MediaLoaderUriUtils.id2Uri(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId));
                                item.setMediaTypeTag(getMediaType());
                                item.setExtension(MediaLoaderFileUtils.getFileExtension(path));
                                folder = new PhotoFolder();
                                folder.setId(folderId);
                                folder.setName(folderName);
                                if (folders.contains(folder)) {
                                    folders.get(folders.indexOf(folder)).addItem(item);
                                } else {
                                    folder.setCover(path);
                                    folder.setCoverUri(item.getUri());
                                    folder.addItem(item);
                                    folders.add(folder);
                                }
                                allItemFolder.addItem(item);
                            }
                            allItemFolder.getItems().addAll(0, mHeadItem);
                            if (allItemFolder.getItems().size() > mHeadItem.size()) {
                                allItemFolder.setCover(allItemFolder.getItems().get(mHeadItem.size()).getPath());
                                allItemFolder.setName("全部图片");
                                folders.add(0, allItemFolder);
                            }
                        }
                        onResult(new PhotoResult(folders));
                    }

                });
    }

    @Override
    public void onItemClick(int position) {
        if (getCurrentItemList().get(position) instanceof AddPhotoItem) {
            takePhoto();
        } else {
            new SlcMpMediaBrowseUtils.Builder().setCurrentPosition(position).setEdit(true)
                    .setPhoto(getCurrentItemList().get(position)).build(mMediaPickerDelegate.getContext());
        }
    }

    @Override
    public void onItemChildClick(int position) {
        if (getCurrentItemList().get(position) instanceof AddPhotoItem) {
            takePhoto();
        } else {
            new SlcMpMediaBrowseUtils.Builder().setCurrentPosition(position).setEdit(true)
                    .setPhoto(getCurrentItemList().get(position)).build(mMediaPickerDelegate.getContext());
        }
    }

    /**
     * 拍照
     */
    protected void takePhoto() {
        SlcMpFilePickerUtils.takePhoto(mMediaPickerDelegate.getContext(), (ActivityResultCaller) mMediaPickerDelegate.getContext(), result -> {
            if (result != null) {
                Cursor cursor = mMediaPickerDelegate.getContext().getContentResolver().query(result, new String[]{}, null, new String[]{}, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String folderId = cursor.getString(cursor.getColumnIndexOrThrow(BUCKET_ID));
                        String folderName = cursor.getString(cursor.getColumnIndexOrThrow(BUCKET_DISPLAY_NAME));
                        int imageId = cursor.getInt(cursor.getColumnIndexOrThrow(_ID));
                        String name = cursor.getString(cursor.getColumnIndexOrThrow(DISPLAY_NAME));
                        long size = cursor.getLong(cursor.getColumnIndexOrThrow(SIZE));
                        String path = cursor.getString(cursor.getColumnIndexOrThrow(DATA));
                        long modified = cursor.getLong(cursor.getColumnIndexOrThrow(DATE_MODIFIED));
                        int width = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH));
                        int height = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT));
                        PhotoItem item = new PhotoItem(imageId, name, path, size, modified, width, height);
                        item.setUri(MediaLoaderUriUtils.id2Uri(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId));
                        item.setMediaTypeTag(getMediaType());
                        item.setExtension(MediaLoaderFileUtils.getFileExtension(path));

                        PhotoFolder folder = new PhotoFolder();
                        folder.setId(folderId);
                        folder.setName(folderName);
                        if (mResult.getFolders().contains(folder)) {
                            mResult.getFolders().get(mResult.getFolders().indexOf(folder)).getItems().add(0, item);
                        } else {
                            folder.setCover(path);
                            folder.setCoverUri(item.getUri());
                            folder.addItem(item);
                            mResult.getFolders().add(folder);
                        }
                        mCurrentItemList.add(mHeadItem.size(), item);
                        mResult.getAllItemFolder().getItems().add(mHeadItem.size(), item);
                    }
                    cursor.close();
                    if (resultListenerWeakReference.get() != null) {
                        resultListenerWeakReference.get().onLoadResult(getCurrentItemList());
                    }
                }
            }
        });
        /*SlcMp.getInstance().takePhoto(mMediaPickerDelegate.getContext(), new Observer<ExtensionMap>() {
            @Override
            public void onChanged(ExtensionMap extensionMap) {
                SlcMpOperateModel.getInstance().removeObserver(this);
                switch (extensionMap.getResultCode()) {
                    case Activity.RESULT_OK:
                        final File file = UriUtils.uri2File(mMediaPickerDelegate.getContext(), extensionMap.getUri());
                        MediaLoader.getLoader().loadPhotos((FragmentActivity) mMediaPickerDelegate.getContext(),
                                new OnPhotoLoaderBaseCallBack<IPhotoResult>(new IFileProperty() {
                                    @Override
                                    public String createSelection() {
                                        StringBuilder extensionBuilder = new StringBuilder();
                                        extensionBuilder.append("(").append(DATA).append(" == ? ").append(")");
                                        return extensionBuilder.toString();
                                    }

                                    @Override
                                    public String[] createSelectionArgs() {
                                        return new String[]{file.getPath()};
                                    }

                                    @Override
                                    public String createSortOrderSql() {
                                        return MediaStore.MediaColumns.DATE_MODIFIED + " DESC";
                                    }
                                }) {
                                    @Override
                                    public void onLoadFinish(Loader<Cursor> loader, Cursor data) {
                                        if (data != null) {
                                            while (data.moveToNext()) {
                                                String folderId = data.getString(data.getColumnIndexOrThrow(BUCKET_ID));
                                                String folderName = data.getString(data.getColumnIndexOrThrow(BUCKET_DISPLAY_NAME));
                                                int imageId = data.getInt(data.getColumnIndexOrThrow(_ID));
                                                String name = data.getString(data.getColumnIndexOrThrow(DISPLAY_NAME));
                                                long size = data.getLong(data.getColumnIndexOrThrow(SIZE));
                                                String path = data.getString(data.getColumnIndexOrThrow(DATA));
                                                long modified = data.getLong(data.getColumnIndexOrThrow(DATE_MODIFIED));
                                                int width = data.getInt(data.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH));
                                                int height = data.getInt(data.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT));
                                                PhotoItem item = new PhotoItem(imageId, name, path, size, modified, width, height);
                                                item.setMediaTypeTag(getMediaType());
                                                item.setExtension(MediaLoaderFileUtils.getFileExtension(path));

                                                PhotoFolder folder = new PhotoFolder();
                                                folder.setId(folderId);
                                                folder.setName(folderName);
                                                if (mResult.getFolders().contains(folder)) {
                                                    mResult.getFolders().get(mResult.getFolders().indexOf(folder)).getItems().add(0, item);
                                                } else {
                                                    folder.setCover(path);
                                                    folder.addItem(item);
                                                    mResult.getFolders().add(folder);
                                                }
                                                mCurrentItemList.add(mHeadItem.size(), item);
                                                mResult.getAllItemFolder().getItems().add(mHeadItem.size(), item);
                                            }
                                            onResult(mResult);
                                        }
                                    }

                                    @Override
                                    public void onResult(IPhotoResult result) {
                                        if (resultListenerWeakReference.get() != null) {
                                            resultListenerWeakReference.get().onLoadResult(getCurrentItemList());
                                        }
                                    }
                                });
                        break;
                }
            }
        });*/
    }

    @Override
    public Object onSelectEvent(int eventCode, SelectEvent event) {
        switch (eventCode) {
            case SelectEvent.EVENT_LISTENER_MEDIA_TYPE:
                return SlcMp.MEDIA_TYPE_PHOTO;
        }
        return null;
    }

}
