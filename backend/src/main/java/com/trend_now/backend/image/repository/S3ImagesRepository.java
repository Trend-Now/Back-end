package com.trend_now.backend.image.repository;

import com.trend_now.backend.image.domain.S3Images;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface S3ImagesRepository extends JpaRepository<S3Images, Long> {

    List<S3Images> findAllByPosts_Id(Long postsId);

}
