package com.dsi.studyhub.dtos;

import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;
import java.util.List;
public record PostReqDto(String title, String content, List<MultipartFile> imgs, Long communityId) implements Serializable {
}
