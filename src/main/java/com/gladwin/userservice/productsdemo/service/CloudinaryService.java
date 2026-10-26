package com.gladwin.userservice.productsdemo.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String uploadImage(
            MultipartFile file,
            String mainCategory,
            String subCategory) throws IOException {

        // Make category names safe for Cloudinary folder names
        String safeMainCategory = mainCategory.replaceAll("[^a-zA-Z0-9_-]", "-");
        String safeSubCategory = subCategory.replaceAll("[^a-zA-Z0-9_-]", "-");

        // Create the Cloudinary folder structure
        String folder = "gladwin-product-suite/"
                + safeMainCategory
                + "/"
                + safeSubCategory;

        Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", folder
                )
        );

        return uploadResult.get("secure_url").toString();
    }
    public String uploadProfileImage(MultipartFile file) throws IOException {

        Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "gladwin-product-suite/profile-images"
                )
        );

        return uploadResult.get("secure_url").toString();
    }
}