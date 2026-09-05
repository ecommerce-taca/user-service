package com.ecommerce.authuser.kyc.application.admin.queue;

import java.util.List;

public record AdminKycQueueResult(
        List<AdminKycQueueItem> items,
        int page,
        int size,
        long total,
        int totalPages
) {
}
