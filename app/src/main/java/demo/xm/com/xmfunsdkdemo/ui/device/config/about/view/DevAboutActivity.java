package demo.xm.com.xmfunsdkdemo.ui.device.config.about.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.lib.ECONFIG;
import com.lib.sdk.bean.StringUtils;
import com.lib.sdk.bean.SysDevAbilityInfoBean;
import com.manager.db.DevDataCenter;
import com.manager.db.XMDevInfo;
import com.manager.sysability.SysAbilityManager;
import com.utils.FileUtils;
import com.utils.XUtils;
import com.xm.ui.widget.ListSelectItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import demo.xm.com.xmfunsdkdemo.R;
import demo.xm.com.xmfunsdkdemo.ui.device.config.BaseConfigActivity;
import demo.xm.com.xmfunsdkdemo.ui.device.config.about.listener.DevAboutContract;
import demo.xm.com.xmfunsdkdemo.ui.device.config.about.presenter.DevAboutPresenter;
import io.reactivex.annotations.Nullable;

/**
 * 关于设备界面,包含设备基本信息(序列号,设备型号,硬件版本,软件版本,
 * 发布时间,设备时间,运行时间,网络模式,云连接状态,固件更新及恢复出厂设置)
 * Created by jiangping on 2018-10-23.
 */
public class DevAboutActivity extends BaseConfigActivity<DevAboutPresenter> implements DevAboutContract.IDevAboutView, View.OnClickListener {
    private TextView devSnText = null;
    private TextView devModelText = null;
    private TextView devHWVerText = null;
    private TextView devSWVerText = null;
    private TextView devPubDateText = null;
    private TextView devPubTimeText = null;
    private TextView devRunTimeText = null;
    private TextView devNatCodeText = null;
    private TextView devNatStatusText = null;
    private ImageView devSNCodeImg = null;
    private TextView devUpdateText = null;
    private ListSelectItem lsiDevUpgrade;
    private ListSelectItem lsiDevPid;//设备PID信息
    private ListSelectItem lsiDevLocalUpgrade;//本地升级
    private ListSelectItem lsiSyncDevTime;//同步时间
    private ListSelectItem lsiSyncDevTimeZone;//同步时区
    private ListSelectItem lsiOemId;//OEMID信息
    private Button defaltConfigBtn = null;
    private TextView tvDevInfo;
    private boolean isLocalUpgrade;//是否为本地升级
    private static final int SYS_LOCAL_FILE_REQUEST_CODE = 0x08;

    @Override
    public DevAboutPresenter getPresenter() {
        return new DevAboutPresenter(this);

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_device_system_info);
        initView();
        initData();
    }

    private void initView() {
        titleBar = findViewById(R.id.layoutTop);
        titleBar.setTitleText(getString(R.string.device_system_info));
        titleBar.setLeftClick(this);

        devSnText = findViewById(R.id.textDeviceSN);
        devModelText = findViewById(R.id.textDeviceModel);
        devHWVerText = findViewById(R.id.textDeviceHWVer);
        devSWVerText = findViewById(R.id.textDeviceSWVer);
        devPubDateText = findViewById(R.id.textDevicePubDate);

        devRunTimeText = findViewById(R.id.textDeviceRunTime);
        devNatCodeText = findViewById(R.id.textDeviceNatCode);
        devNatStatusText = findViewById(R.id.textDeviceNatStatus);
        devSNCodeImg = findViewById(R.id.imgDeviceQRCode);
        devUpdateText = findViewById(R.id.textDeviceUpgrade);
        defaltConfigBtn = findViewById(R.id.defealtconfig);
        defaltConfigBtn.setOnClickListener(this);
        tvDevInfo = findViewById(R.id.tv_device_info);

        lsiDevUpgrade = findViewById(R.id.lsi_check_dev_upgrade);
        lsiDevUpgrade.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (presenter.isDevUpgradeEnable()) {
                    presenter.startDevUpgrade();
                } else {
                    showToast(getString(R.string.already_latest), Toast.LENGTH_LONG);
                }
            }
        });

        lsiDevLocalUpgrade = findViewById(R.id.lsi_local_dev_upgrade);
        lsiDevLocalUpgrade.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(intent, SYS_LOCAL_FILE_REQUEST_CODE);
            }
        });

        lsiDevPid = findViewById(R.id.lsi_dev_pid);

        lsiSyncDevTime = findViewById(R.id.lsi_sync_dev_time);
        lsiSyncDevTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWaitDialog();
                presenter.syncDevTime();
            }
        });

        lsiSyncDevTimeZone = findViewById(R.id.lsi_sync_dev_time_zone);
        lsiSyncDevTimeZone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWaitDialog();
                presenter.syncDevTimeZone();
            }
        });

        lsiOemId = findViewById(R.id.lsi_dev_oemid);
    }

    private void initData() {
        presenter.getDevInfo();
        presenter.checkDevUpgrade();
        presenter.getDevOemId(this);

        XMDevInfo xmDevInfo = DevDataCenter.getInstance().getDevInfo(presenter.getDevId());
        if (xmDevInfo != null) {
            lsiDevPid.setRightText(StringUtils.isStringNULL(xmDevInfo.getPid()) ? "" : xmDevInfo.getPid());
        }
    }

    @Override
    public void onClick(View view) {

    }

    @Override
    public void onUpdateView(String result) {
        tvDevInfo.setText(result);
    }

    @Override
    public void onCheckDevUpgradeResult(boolean isSuccess, boolean isNeedUpgrade) {
        if (isNeedUpgrade) {
            lsiDevUpgrade.setRightText(getString(R.string.have_new_version_click_to_upgrade));
        } else {
            lsiDevUpgrade.setRightText(getString(R.string.already_latest));
        }
    }

    @Override
    public void onDevUpgradeProgressResult(int upgradeState, int progress) {
        switch (upgradeState) {
            //正在下载升级包
            case ECONFIG.EUPGRADE_STEP_DOWN:
                if (isLocalUpgrade) {
                    lsiDevLocalUpgrade.setRightText(getString(R.string.download_dev_firmware) + ":" + progress);
                } else {
                    lsiDevUpgrade.setRightText(getString(R.string.download_dev_firmware) + ":" + progress);
                }
                break;
            //正在上传
            case ECONFIG.EUPGRADE_STEP_UP:
                if (isLocalUpgrade) {
                    lsiDevLocalUpgrade.setRightText(getString(R.string.upload_dev_firmware) + ":" + progress);
                } else {
                    lsiDevUpgrade.setRightText(getString(R.string.upload_dev_firmware) + ":" + progress);
                }
                break;
            //正在升级
            case ECONFIG.EUPGRADE_STEP_UPGRADE:
                if (isLocalUpgrade) {
                    lsiDevLocalUpgrade.setRightText(getString(R.string.dev_upgrading) + ":" + progress);
                } else {
                    lsiDevUpgrade.setRightText(getString(R.string.dev_upgrading) + ":" + progress);
                }
                break;
            //升级完成
            case ECONFIG.EUPGRADE_STEP_COMPELETE:
                if (isLocalUpgrade) {
                    lsiDevLocalUpgrade.setRightText(getString(R.string.completed_dev_upgrade));
                } else {
                    lsiDevUpgrade.setRightText(getString(R.string.completed_dev_upgrade));
                }

                isLocalUpgrade = false;
                break;
            default:
                break;
        }
    }

    @Override
    public void syncDevTimeZoneResult(boolean isSuccess, int errorId) {
        hideWaitDialog();
        showToast(isSuccess ? getString(R.string.set_dev_config_success) : getString(R.string.set_dev_config_failed) + ":" + errorId, Toast.LENGTH_LONG);
    }

    @Override
    public void syncDevTimeResult(boolean isSuccess, int errorId) {
        hideWaitDialog();
        showToast(isSuccess ? getString(R.string.set_dev_config_success) : getString(R.string.set_dev_config_failed) + ":" + errorId, Toast.LENGTH_LONG);
    }

    @Override
    public void onGetDevOemIdResult(String oemId) {
        if (oemId != null) {
            lsiOemId.setRightText(oemId);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @androidx.annotation.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SYS_LOCAL_FILE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();
                String filePath = presenter.saveFileFromUri(this, uri);
                presenter.startDevLocalUpgrade(filePath);
                isLocalUpgrade = true;
            }
        }
    }
}
