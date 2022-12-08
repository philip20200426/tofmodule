package com.nbd.autofocus;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.nbd.autofocus.tofmodule.TofHelper;
import com.nbd.tofmodule.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
        private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Used to load the 'tofmodule' library on application startup.
        TofHelper.test = 1;
        Log.d("TofModule", "---------------------------------");
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        TofHelper mTofHelper = TofHelper.getInstance();

        TofHelper.ParamInfo mParamInfo =  mTofHelper.getmParamInfo();
        byte[] b = new byte[10];
        for (int i = 0; i < 9; i++) {
            b[i] = (byte) (i + 97);
        }
        mParamInfo.data = b;
        mParamInfo.device = "i2c0";
        mParamInfo.resolutionValue = 64;
        mParamInfo.rangingMode = 1;
        mParamInfo.frequencyHz = 15;
        mParamInfo.targetOrder = 1;
        if (mTofHelper.openModule() == 0) {
            mTofHelper.setParamInfo(mParamInfo);
            TofHelper.ResultsData mResultsData = mTofHelper.getResultsData();
            for (int i = 0; i < mParamInfo.resolutionValue; i++) {
                Log.i(" TofModule", "Zone : " + i +
                        ", Target status : " + mResultsData.targetStatus[i] +
                        ", distance : " + mResultsData.distanceMm[i]);
            }
        } else {
            Log.e("TofModule", " TOF open error");
        }
    }

    public void TofCallback() {
        Log.e("philip", "TofCallback");
    }
}