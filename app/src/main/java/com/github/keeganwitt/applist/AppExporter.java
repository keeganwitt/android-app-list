package com.github.keeganwitt.applist;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class AppExporter {
    private static final String TAG = AppExporter.class.getSimpleName();

    private final AppCompatActivity activity;
    private final AppInfoAdapter appInfoAdapter;
    private final PackageManager packageManager;
    private final android.app.usage.UsageStatsManager usageStatsManager;
    private AppInfoField selectedAppInfoField;
    private String currentExportType;

    private final ActivityResultLauncher<Intent> createFileLauncher;

    public AppExporter(AppCompatActivity activity, AppInfoAdapter appInfoAdapter, PackageManager packageManager, android.app.usage.UsageStatsManager usageStatsManager) {
        this.activity = activity;
        this.appInfoAdapter = appInfoAdapter;
        this.packageManager = packageManager;
        this.usageStatsManager = usageStatsManager;

        this.createFileLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            if ("xml".equals(currentExportType)) {
                                writeXmlToFile(uri);
                            } else if ("html".equals(currentExportType)) {
                                writeHtmlToFile(uri);
                            }
                        }
                    }
                });
    }

    public void export(AppInfoField selectedAppInfoField) {
        this.selectedAppInfoField = selectedAppInfoField;
        showExportDialog();
    }

    private void showExportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_export_type, null);
        builder.setView(dialogView);

        RadioGroup radioGroup = dialogView.findViewById(R.id.export_radio_group);

        builder.setPositiveButton(R.string.export, (dialog, which) -> {
            int selectedId = radioGroup.getCheckedRadioButtonId();
            if (selectedId == R.id.radio_xml) {
                initiateExport("xml");
            } else if (selectedId == R.id.radio_html) {
                initiateExport("html");
            }
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void initiateExport(String type) {
        if (appInfoAdapter == null || appInfoAdapter.getItemCount() == 0) {
            Toast.makeText(activity, activity.getString(R.string.export_no_apps), Toast.LENGTH_SHORT).show();
            return;
        }

        currentExportType = type;
        String timestamp = new java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.US)
                .format(new java.util.Date());
        String appInfoType = selectedAppInfoField.name().toLowerCase();
        String fileName = "apps_" + appInfoType + "_" + timestamp + "." + type;

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(type.equals("xml") ? "text/xml" : "text/html");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        createFileLauncher.launch(intent);
    }

    private void writeXmlToFile(Uri uri) {
        try {
            java.io.OutputStream outputStream = activity.getContentResolver().openOutputStream(uri);
            if (outputStream != null) {
                XmlSerializer serializer = Xml.newSerializer();
                serializer.setOutput(outputStream, "UTF-8");
                serializer.startDocument("UTF-8", true);
                serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);

                serializer.startTag("", "apps");

                for (int i = 0; i < appInfoAdapter.getItemCount(); i++) {
                    AppInfo app = appInfoAdapter.getCurrentList().get(i);
                    String appName = app.getApplicationInfo().loadLabel(packageManager).toString();
                    String packageName = app.getApplicationInfo().packageName;
                    String infoType = selectedAppInfoField.name();
                    String infoValue = "";

                    try {
                        infoValue = app.getTextValue(activity, packageManager, usageStatsManager);
                    } catch (PackageManager.NameNotFoundException e) {
                        // Leave infoValue empty
                    }

                    serializer.startTag("", "app");
                    serializer.startTag("", "appName");
                    serializer.text(appName);
                    serializer.endTag("", "appName");
                    serializer.startTag("", "appPackage");
                    serializer.text(packageName != null ? packageName : "");
                    serializer.endTag("", "appPackage");
                    serializer.startTag("", "appInfoType");
                    serializer.text(infoType);
                    serializer.endTag("", "appInfoType");
                    serializer.startTag("", "appInfoValue");
                    serializer.text(infoValue != null ? infoValue : "");
                    serializer.endTag("", "appInfoValue");
                    serializer.endTag("", "app");
                }

                serializer.endTag("", "apps");
                serializer.endDocument();
                outputStream.close();

                Toast.makeText(activity, activity.getString(R.string.export_successful), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error exporting XML", e);
            Toast.makeText(activity, activity.getString(R.string.export_failed) + " " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void writeHtmlToFile(Uri uri) {
        try {
            java.io.OutputStream outputStream = activity.getContentResolver().openOutputStream(uri);
            if (outputStream != null) {
                OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
                StringBuilder htmlBuilder = new StringBuilder();
                htmlBuilder.append("<!DOCTYPE html>\n");
                htmlBuilder.append("<html>\n");
                htmlBuilder.append("<head>\n");
                htmlBuilder.append("<meta charset=\"UTF-8\">\n");
                htmlBuilder.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
                htmlBuilder.append("<title>App List</title>\n");
                htmlBuilder.append("<style>\n");
                htmlBuilder.append("body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }\n");
                htmlBuilder.append("h1 { text-align: center; color: #333; }\n");
                htmlBuilder.append(".app-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 10px; padding: 10px; }\n");
                htmlBuilder.append(".app-item { background: white; border-radius: 8px; padding: 10px; text-align: center; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
                htmlBuilder.append(".app-icon { width: 50px; height: 50px; margin: 0 auto; }\n");
                htmlBuilder.append(".app-name { font-weight: bold; margin-top: 8px; word-wrap: break-word; }\n");
                htmlBuilder.append(".package-name { font-style: italic; font-size: 0.9em; color: #666; margin-top: 4px; word-wrap: break-word; }\n");
                htmlBuilder.append(".app-info { margin-top: 4px; color: #333; word-wrap: break-word; }\n");
                htmlBuilder.append("@media (min-width: 600px) { .app-grid { grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); } }\n");
                htmlBuilder.append("</style>\n");
                htmlBuilder.append("</head>\n");
                htmlBuilder.append("<body>\n");
                htmlBuilder.append("<h1>App List</h1>\n");
                htmlBuilder.append("<div class=\"app-grid\">\n");

                for (int i = 0; i < appInfoAdapter.getItemCount(); i++) {
                    AppInfo app = appInfoAdapter.getCurrentList().get(i);
                    String appName = Html.escapeHtml(app.getApplicationInfo().loadLabel(packageManager).toString());
                    String packageName = Html.escapeHtml(app.getApplicationInfo().packageName);
                    String infoValue = "";

                    try {
                        infoValue = Html.escapeHtml(app.getTextValue(activity, packageManager, usageStatsManager));
                    } catch (PackageManager.NameNotFoundException e) {
                        infoValue = "";
                    }

                    String iconBase64 = drawableToBase64(app.getApplicationInfo().loadIcon(packageManager));

                    htmlBuilder.append("<div class=\"app-item\">\n");
                    htmlBuilder.append("<img class=\"app-icon\" src=\"data:image/png;base64,").append(iconBase64).append("\" alt=\"App Icon\">\n");
                    htmlBuilder.append("<div class=\"app-name\">").append(appName).append("</div>\n");
                    htmlBuilder.append("<div class=\"package-name\">").append(packageName).append("</div>\n");
                    htmlBuilder.append("<div class=\"app-info\">").append(infoValue).append("</div>\n");
                    htmlBuilder.append("</div>\n");
                }

                htmlBuilder.append("</div>\n");
                htmlBuilder.append("</body>\n");
                htmlBuilder.append("</html>\n");

                writer.write(htmlBuilder.toString());
                writer.close();
                outputStream.close();

                Toast.makeText(activity, activity.getString(R.string.export_successful), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error exporting HTML", e);
            Toast.makeText(activity, activity.getString(R.string.export_failed) + " " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String drawableToBase64(Drawable drawable) {
        if (drawable == null) {
            return "";
        }
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }
}