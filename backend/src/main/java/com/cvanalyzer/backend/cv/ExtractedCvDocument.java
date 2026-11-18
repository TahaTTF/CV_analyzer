package com.cvanalyzer.backend.cv;

public record ExtractedCvDocument(CvDocumentType type,String text,Integer pageCount) {}
