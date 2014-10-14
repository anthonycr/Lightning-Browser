package info.guardianproject.onionkit.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;

public class CertDisplayActivity extends Activity {

    private AlertDialog ad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String issuer = getIntent().getStringExtra("issuer");
        String fingerprint = getIntent().getStringExtra("fingerprint");
        String subject = getIntent().getStringExtra("subject");
        String issuedOn = getIntent().getStringExtra("issued");
        String expiresOn = getIntent().getStringExtra("expires");
        String msg = getIntent().getStringExtra("msg");

        StringBuilder sb = new StringBuilder();

        if (msg != null)
            sb.append(msg).append("\n\n");

        if (subject != null)
            sb.append("Certificate: ").append(subject).append("\n\n");

        if (issuer != null)
            sb.append("Issued by: ").append(issuer).append("\n\n");

        if (fingerprint != null)
            sb.append("SHA1 Fingerprint: ").append(fingerprint).append("\n\n");

        if (issuedOn != null)
            sb.append("Issued: ").append(issuedOn).append("\n\n");

        if (expiresOn != null)
            sb.append("Expires: ").append(expiresOn).append("\n\n");

        showDialog(sb.toString());
    }

    private void showDialog(String msg) {

        ad = new AlertDialog.Builder(this).setTitle("Certificate Info").setMessage(msg).show();

        ad.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface arg0) {

                CertDisplayActivity.this.finish();

            }

        });

    }

    @Override
    protected void onPause() {
        super.onPause();

        if (ad != null)
            ad.cancel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (ad != null)
            ad.cancel();

    }

}
