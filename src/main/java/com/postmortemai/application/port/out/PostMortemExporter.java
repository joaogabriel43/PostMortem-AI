package com.postmortemai.application.port.out;

import com.postmortemai.application.dto.ExportFormat;
import com.postmortemai.domain.model.PostMortem;

public interface PostMortemExporter {
    byte[] export(PostMortem postMortem);
    boolean supports(ExportFormat format);
}
