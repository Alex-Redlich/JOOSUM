package com.addShot.zoosum.entity.embedded;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

//유저-동물 복합키 클래스
@Embeddable
public class UserAnimalId implements Serializable {

	private String userId;
	private Long animalId;

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		UserAnimalId that = (UserAnimalId) o;
		return Objects.equals(userId, that.userId) && Objects.equals(animalId,
			that.animalId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, animalId);
	}

}
