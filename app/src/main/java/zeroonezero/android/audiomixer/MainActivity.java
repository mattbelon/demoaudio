package zeroonezero.android.audiomixer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaExtractor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import zeroonezero.android.audio_mixer.AudioMixer;
import zeroonezero.android.audio_mixer.input.AudioInput;
import zeroonezero.android.audio_mixer.input.BlankAudioInput;
import zeroonezero.android.audio_mixer.input.GeneralAudioInput;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 0;
    private static final int AUDIO_CHOOSE_REQUEST_CODE = 1;
    private Activity activity;
    private AssetManager myAsset;
    private String outputPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "audio_mixer_output.mp3";

    private List<Input> inputs = new ArrayList<>();
    private AudioMixer audioMixer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = this;

        findViewById(R.id.add_audio_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    openChooser();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        findViewById(R.id.mix_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (inputs.size() < 1) {
                    Toast.makeText(activity, "Add at least one audio.", Toast.LENGTH_SHORT).show();
                } else {
                    startMixing();
                }
            }
        });

        findViewById(R.id.remove_audio_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (inputs.size() > 0) {
                    inputs.remove(inputs.size() - 1);
                    Toast.makeText(activity, "Last audio removed. Number of inputs: " + inputs.size(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(activity, "No audio added.", Toast.LENGTH_SHORT).show();
                }
            }
        });


    }

    private void startMixing() {
        //For showing progress
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Mixing audio...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(false);
        progressDialog.setProgress(0);

        try {
            audioMixer = new AudioMixer(outputPath);

            for (Input input : inputs) {
                AudioInput audioInput;
                if (input.uri != null) {
                    GeneralAudioInput ai = new GeneralAudioInput(activity, input.uri, null);
                    ai.setStartOffsetUs(input.startOffsetUs);
                    ai.setStartTimeUs(input.startTimeUs); // optional
                    ai.setEndTimeUs(input.endTimeUs); // optional
                    //ai.setVolume(0.5f); //optional

                    audioInput = ai;
                } else {
                    audioInput = new BlankAudioInput(5000000);
                }
                audioMixer.addDataSource(audioInput);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //audioMixer.setSampleRate(44100);  // optional
        //audioMixer.setBitRate(128000); // optional
        //audioMixer.setChannelCount(2); // 1 or 2 // optional
        //audioMixer.setLoopingEnabled(true); // Only works for parallel mixing
        audioMixer.setMixingType(AudioMixer.MixingType.PARALLEL);
        audioMixer.setProcessingListener(new AudioMixer.ProcessingListener() {
            @Override
            public void onProgress(double progress) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.setProgress((int) (progress * 100));
                    }
                });
            }

            @Override
            public void onEnd() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.setProgress(100);
                        progressDialog.dismiss();
                        Toast.makeText(activity, "Success!!! Ouput path: " + outputPath, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        progressDialog.setButton(DialogInterface.BUTTON_POSITIVE, "End", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                audioMixer.stop();
                audioMixer.release();
            }
        });

        try {
            audioMixer.start();
            audioMixer.processAsync();
            progressDialog.show();
        } catch (IOException e) {
            audioMixer.release();
        }
    }

    /*public void openChooser(){
        Intent intent = new Intent();
        intent.setType("audio/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, AUDIO_CHOOSE_REQUEST_CODE);
    }*/
    public void openChooser() throws IOException {
        String ocean = String.valueOf(Uri.parse("android.resource://zeroonezero.android.audiomixer/raw/ocean"));
        //Uri fire = Uri.parse("android.resource://zeroonezero.android.audiomixer/raw/firesticks");
        String fire = String.valueOf(Uri.parse("android.resource://zeroonezero.android.audiomixer/raw/firesticks"));

       /* AssetManager myAsset = null;

        assert false;
        InputStream audio = myAsset.open(ocean);*/

        //AudioInput input1 = new GeneralAudioInput(ocean);
        AudioInput input2 = new GeneralAudioInput(fire);

        String outputPath = Environment.getDownloadCacheDirectory().getAbsolutePath()
                + "/" + "temporal.mp3"; // for example

// Assuming a raw resource located at "res/raw/test_audio.mp3"

        MediaExtractor extractor = new MediaExtractor();
        AssetFileDescriptor afd = getResources().openRawResourceFd(R.raw.ocean);
        try {
            extractor.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
        } catch (IOException e) {
            e.printStackTrace();
        }

        audioMixer.setMixingType(AudioMixer.MixingType.PARALLEL);
//it is for setting up the all the things
        audioMixer.start();
        //starting real processing
        audioMixer.processAsync();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AUDIO_CHOOSE_REQUEST_CODE && resultCode == RESULT_OK) {
            try {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(activity, data.getData());
                String dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                long duration = Integer.parseInt(dur) * 1000L; // milli to micro second
                retriever.release();

                Input input = new Input();
                input.uri = data.getData();
                input.durationUs = duration;
                inputs.add(input);
                Toast.makeText(activity, inputs.size() + " input(s) added.", Toast.LENGTH_SHORT).show();

                AudioInputSettingsDialog dialog = new AudioInputSettingsDialog(activity, input);
                dialog.setCancelable(false);
                dialog.show();

            } catch (Exception o) {
                Toast.makeText(activity, "Input not added.", Toast.LENGTH_SHORT).show();
            }
        }
    }


}