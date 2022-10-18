package org.vosk.ime;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.core.content.ContextCompat;

import org.vosk.ime.settingsfragments.ModelsAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Tools {

    public static boolean isMicrophonePermissionGranted(Activity activity) {
        int permissionCheck = ContextCompat.checkSelfPermission(activity.getApplicationContext(),
                Manifest.permission.RECORD_AUDIO);
        return permissionCheck == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isIMEEnabled(Activity activity) {
        InputMethodManager imeManager = (InputMethodManager) activity.getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        for (InputMethodInfo i : imeManager.getEnabledInputMethodList()) {
            if (i.getPackageName().equals(activity.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    public static void downloadModelFromLink(ModelLink model, FileDownloadService.OnDownloadStatusListener listener, Context context) {
        String serverFilePath = model.link;

        File tempFolder = Constants.getTemporaryDownloadLocation(context);
        if (!tempFolder.exists()) {
            tempFolder.mkdirs();
        }

        String fileName = model.link.substring(model.link.lastIndexOf('/') + 1); // file name
        File tempFile = new File(tempFolder, fileName);

        String localPath = tempFile.getAbsolutePath();

        File modelFolder = Constants.getDirectoryForModel(context, model.locale);

        if (!modelFolder.exists()) {
            modelFolder.mkdirs();
        }

        String unzipPath = modelFolder.getAbsolutePath();

        FileDownloadService.DownloadRequest downloadRequest = new FileDownloadService.DownloadRequest(serverFilePath, localPath, true);
        downloadRequest.setRequiresUnzip(true);
        downloadRequest.setDeleteZipAfterExtract(true);
        downloadRequest.setUnzipAtFilePath(unzipPath);

        FileDownloadService.FileDownloader downloader = FileDownloadService.FileDownloader.getInstance(downloadRequest, listener);
        downloader.download(context);
    }

    public static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }

    public static Map<Locale, List<Model>> getInstalledModelsMap(Context context) {
        Map<Locale, List<Model>> localeMap = new HashMap<>();

        File modelsDir = Constants.getModelsDirectory(context);

        if (!modelsDir.exists()) return localeMap;

        for (File localeFolder : modelsDir.listFiles()) {
            if (!localeFolder.isDirectory()) continue;
            Locale locale = Locale.forLanguageTag(localeFolder.getName());
            List<Model> models = new ArrayList<>();
            for (File modelFolder : localeFolder.listFiles()) {
                if (!modelFolder.isDirectory()) continue;
                String name = modelFolder.getName();
                Model model = new Model(modelFolder.getAbsolutePath(), locale, name);
                models.add(model);
            }
            localeMap.put(locale, models);
        }
        return localeMap;
    }

    public static List<Model> getInstalledModelsList(Context context){
        List<Model> models = new ArrayList<>();

        File modelsDir = Constants.getModelsDirectory(context);

        if (!modelsDir.exists()) return models;

        for (File localeFolder : modelsDir.listFiles()) {
            if (!localeFolder.isDirectory()) continue;
            Locale locale = Locale.forLanguageTag(localeFolder.getName());
            for (File modelFolder : localeFolder.listFiles()) {
                if (!modelFolder.isDirectory()) continue;
                String name = modelFolder.getName();
                Model model = new Model(modelFolder.getAbsolutePath(), locale, name);
                models.add(model);
            }
        }
        return models;
    }

    public static List<ModelsAdapter.Data> getModelsData(Context context) {
        List<ModelsAdapter.Data> data = new ArrayList<>();
        Map<Locale, List<Model>> installedModels = getInstalledModelsMap(context);
        for (ModelLink link : ModelLink.values()) {
            boolean found = false;
            if (installedModels.containsKey(link.locale)) {
                List<Model> localeModels = installedModels.get(link.locale);
                for (int i = 0; i < localeModels.size(); i++) {
                    Model model = localeModels.get(i);
                    if (model.filename.equals(link.getFilename())) {
                        data.add(new ModelsAdapter.Data(link, model));
                        localeModels.remove(i);
                        found = true;
                        break;
                    }
                }
            }
            if (!found)
                data.add(new ModelsAdapter.Data(link));
        }
        for (List<Model> models : installedModels.values()) {
            for (Model model : models) {
                data.add(new ModelsAdapter.Data(model));
            }
        }

        return data;
    }

    public static Model getModelForLink(ModelLink modelLink, Context context) {
        File modelsDir = Constants.getModelsDirectory(context);
        File localeDir = new File(modelsDir, modelLink.locale.toLanguageTag());
        File modelDir = new File(localeDir, modelLink.getFilename());
        if (!localeDir.exists() || modelDir.exists() || !modelDir.isDirectory()) {
            return null;
        }
        return new Model(modelDir.getAbsolutePath(), modelLink.locale, modelLink.getFilename());
    }
}
