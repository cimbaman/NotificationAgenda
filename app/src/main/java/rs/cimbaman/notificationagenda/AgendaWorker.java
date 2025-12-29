package rs.cimbaman.notificationagenda;


import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class AgendaWorker extends Worker {

    public AgendaWorker(@NonNull Context context,
                        @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        AgendaHelper.updateNotificationAgenda(getApplicationContext());
        return Result.success();
    }
}