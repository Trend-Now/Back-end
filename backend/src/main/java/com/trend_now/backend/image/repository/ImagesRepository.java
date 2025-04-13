package com.trend_now.backend.image.repository;

import com.trend_now.backend.image.domain.Images;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImagesRepository extends JpaRepository<Images, Long> {

    List<Images> findAllByPosts_Id(Long postsId);

}
