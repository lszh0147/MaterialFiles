/*
 * Copyright (c) 2018 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.materialfilemanager.filelist;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.arch.lifecycle.ViewModel;
import android.net.Uri;
import android.os.Parcelable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.zhanghai.android.materialfilemanager.filesystem.File;
import me.zhanghai.android.materialfilemanager.filesystem.Files;
import me.zhanghai.android.materialfilemanager.settings.Settings;

public class FileListViewModel extends ViewModel {

    private Trail mTrail = new Trail();
    private MutableLiveData<File> mFileData = new MutableLiveData<>();
    private LiveData<FileListData> mFileListData = Transformations.switchMap(mFileData,
            FileListLiveData::new);
    private MutableLiveData<FileSortOptions> mSortOptionsData = new MutableLiveData<>();
    private MutableLiveData<Set<Uri>> mSelectedUrisData = new MutableLiveData<>();

    public FileListViewModel() {
        // TODO
        File file = Files.ofUri(Uri.parse("file:///storage/emulated/0/Download"));
        navigateTo(null, file.makeBreadcrumbPath());
        mSortOptionsData.setValue(new FileSortOptions(Settings.FILE_LIST_SORT_BY.getEnumValue(),
                Settings.FILE_LIST_SORT_ORDER.getEnumValue(),
                Settings.FILE_LIST_SORT_DIRECTORIES_FIRST.getValue()));
        mSelectedUrisData.setValue(new HashSet<>());
    }

    public void navigateTo(Parcelable lastState, List<File> path) {
        mTrail.navigateTo(lastState, path);
        Files.onTrailChanged(mTrail.getTrail());
        mFileData.setValue(mTrail.getCurrentFile());
    }

    public boolean navigateUp() {
        boolean changed = mTrail.navigateUp();
        if (changed) {
            Files.onTrailChanged(mTrail.getTrail());
            mFileData.setValue(mTrail.getCurrentFile());
        }
        return changed;
    }

    public Parcelable getPendingState() {
        return mTrail.getPendingState();
    }

    public void reload() {
        File file = mFileData.getValue();
        Files.invalidateCache(file);
        mFileData.setValue(file);
    }

    public LiveData<File> getFileData() {
        return mFileData;
    }

    public LiveData<FileListData> getFileListData() {
        return mFileListData;
    }

    public List<File> getTrail() {
        return mTrail.getTrail();
    }

    public int getTrailIndex() {
        return mTrail.getIndex();
    }

    public LiveData<FileSortOptions> getSortOptionsData() {
        return mSortOptionsData;
    }

    public FileSortOptions getSortOptions() {
        return mSortOptionsData.getValue();
    }

    public void setSortOptions(FileSortOptions sortOptions) {
        Settings.FILE_LIST_SORT_BY.putEnumValue(sortOptions.getBy());
        Settings.FILE_LIST_SORT_ORDER.putEnumValue(sortOptions.getOrder());
        Settings.FILE_LIST_SORT_DIRECTORIES_FIRST.putValue(sortOptions.isDirectoriesFirst());
        mSortOptionsData.setValue(sortOptions);
    }

    public LiveData<Set<Uri>> getSelectedUrisData() {
        return mSelectedUrisData;
    }

    public void selectUri(Uri uri, boolean selected) {
        Set<Uri> selectedUris = mSelectedUrisData.getValue();
        boolean changed = selected ? selectedUris.add(uri) : selectedUris.remove(uri);
        if (changed) {
            mSelectedUrisData.setValue(selectedUris);
        }
    }

    public void clearSelectedUris() {
        Set<Uri> selectedUris = mSelectedUrisData.getValue();
        if (selectedUris.isEmpty()) {
            return;
        }
        selectedUris.clear();
        mSelectedUrisData.setValue(selectedUris);
    }
}