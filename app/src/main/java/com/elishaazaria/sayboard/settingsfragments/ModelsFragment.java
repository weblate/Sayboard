package com.elishaazaria.sayboard.settingsfragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.elishaazaria.sayboard.Model;
import com.elishaazaria.sayboard.Tools;
import com.elishaazaria.sayboard.databinding.FragmentModelsBinding;
import com.elishaazaria.sayboard.downloader.FileDownloader;
import com.elishaazaria.sayboard.downloader.messages.DownloadError;
import com.elishaazaria.sayboard.downloader.messages.DownloadProgress;
import com.elishaazaria.sayboard.downloader.messages.DownloadState;
import com.elishaazaria.sayboard.downloader.messages.ModelInfo;
import com.elishaazaria.sayboard.downloader.messages.State;
import com.elishaazaria.sayboard.downloader.messages.Status;
import com.elishaazaria.sayboard.downloader.messages.StatusQuery;
import com.elishaazaria.sayboard.downloader.messages.UnzipProgress;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class ModelsFragment extends Fragment implements ModelsAdapter.ItemClickListener {
    private static final String TAG = "ModelsFragment";

    private FragmentModelsBinding binding;
    private ModelsAdapter adapter;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentModelsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        recyclerView = binding.recyclerView;
        progressBar = binding.progressBar;

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ModelsAdapter(getContext(), Tools.getModelsData(getContext()));
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onItemClick(View view, int position, ModelsAdapter.Data data) {
    }

    @Override
    public void onButtonClicked(View view, int position, ModelsAdapter.Data data) {
        switch (data.getState()) {
            case CLOUD: // Not installed, download
                FileDownloader.downloadModel(data.getModelLink(), requireContext());
                break;
            case INSTALLED: // Installed, delete
                Tools.deleteModel(data.getModel(), getContext());
                boolean removed = data.wasDeleted();
                if (removed) {
                    adapter.removed(data);
                } else {
                    adapter.changed(data);
                }
                break;
            case DOWNLOADING: // Downloading, cancel download
                // TODO: cancel download
                break;
            case QUEUED: // Queued for download, remove from queue
                // TODO: remove from downloading queue
                break;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onState(DownloadState state) {
        ModelsAdapter.Data current = adapter.get(state.info);
        switch (state.state) {
            case DOWNLOAD_STARTED:
                progressBar.setVisibility(View.VISIBLE);
                current.downloading();
                adapter.changed(current);
                break;
            case FINISHED:
                progressBar.setVisibility(View.GONE);
                Model model = Tools.getModelForLink(current.getModelLink(), getContext());
                if (model != null) current.wasInstalled(model);
                adapter.changed(current);
                break;
            case ERROR:
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Download failed for " + current.getFilename(), Toast.LENGTH_SHORT).show();
                current.downloadCanceled();
                adapter.changed(current);
                break;

            case QUEUED:
                current.wasQueued();
                adapter.changed(current);
                break;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStatus(Status status) {
        if (status.current == null) return;
        onState(new DownloadState(status.current, status.state));
        switch (status.state) {
            case DOWNLOAD_STARTED:
                onDownloadProgress(new DownloadProgress(status.current, status.downloadProgress));
                break;
            case UNZIP_STARTED:
                onUnzipProgress(new UnzipProgress(status.current, status.unzipProgress));
                break;
        }

        for (ModelInfo modelInfo :
                status.queued) {
            onState(new DownloadState(modelInfo, State.QUEUED));
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDownloadProgress(DownloadProgress progress) {
        progressBar.setProgress(progress.progress);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnzipProgress(UnzipProgress progress) {
        progressBar.setSecondaryProgress(progress.progress);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDownloadError(DownloadError error) {
//        Toast.makeText(getContext(), error.message, Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        EventBus.getDefault().post(new StatusQuery());
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }
}