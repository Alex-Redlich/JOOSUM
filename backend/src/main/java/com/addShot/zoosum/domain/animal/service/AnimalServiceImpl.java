package com.addShot.zoosum.domain.animal.service;

import com.addShot.zoosum.domain.animal.dto.request.MyAnimalRequest;
import com.addShot.zoosum.domain.animal.dto.request.MyIslandAnimalRequest;
import com.addShot.zoosum.domain.animal.dto.response.AnimalDrawResponse;
import com.addShot.zoosum.domain.animal.dto.response.FlogAnimalResponse;
import com.addShot.zoosum.domain.animal.dto.response.UserAnimalDetailResponse;
import com.addShot.zoosum.domain.animal.dto.response.UserAnimalListResponse;
import com.addShot.zoosum.domain.animal.repository.AnimalMotionRepository;
import com.addShot.zoosum.domain.animal.repository.AnimalRepository;
import com.addShot.zoosum.domain.animal.repository.UserAnimalRepository;
import com.addShot.zoosum.domain.user.repository.UserRepository;
import com.addShot.zoosum.entity.Animal;
import com.addShot.zoosum.entity.AnimalMotion;
import com.addShot.zoosum.entity.UserAnimal;
import com.addShot.zoosum.util.RandomUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnimalServiceImpl implements AnimalService {

	private final UserAnimalRepository userAnimalRepository;
	private final AnimalMotionRepository animalMotionRepository;
	private final AnimalRepository animalRepository;
	private final UserRepository userRepository;

	@Override
	public List<UserAnimalListResponse> getUserAnimalList(String userId) {
		log.info("AnimalService userId : {}", userId);

		List<UserAnimal> userAnimals = userAnimalRepository.findAllByUserId(userId).get();
		List<UserAnimalListResponse> responseList = new ArrayList<>();

		for(UserAnimal ua: userAnimals) {
			UserAnimalListResponse response = UserAnimalListResponse.builder()
				.animalName(ua.getUserAnimalName())
				.fileUrl(animalMotionRepository.findMotion(ua.getAnimal().getAnimalId()).get().getFileUrl())
				.selected(ua.isSelected())
				.build();
			responseList.add(response);
		}
		return responseList;
	}

	@Override
	public UserAnimalDetailResponse getUserAnimalDetail(String userId, Long animalId) {

		Animal animal = animalRepository.findById(animalId).get();
		UserAnimal userAnimal = userAnimalRepository.findByUserIdAndAnimalId(userId, animalId)
			.get();
		AnimalMotion animalMotion = animalMotionRepository.findMotion(animalId).get();

		UserAnimalDetailResponse response = UserAnimalDetailResponse.builder()
			.userAnimalName(userAnimal.getUserAnimalName())
			.createTime(userAnimal.getTime().getCreateTime())
			.timeTogether(userAnimal.getTimeTogether())
			.trashTogether(userAnimal.getTrashTogether())
			.lengthTogether(userAnimal.getLengthTogether())
			.fileUrl(animalMotion.getFileUrl())
			.description(animal.getDescription())
			.build();

		return response;
	}

	@Override
	public AnimalDrawResponse getAnimalDraw() {
		List<Animal> animalList = animalRepository.findAll();
		Animal animal = RandomUtil.getRandomElement(animalList);
		Long animalId = animal.getAnimalId();

		AnimalDrawResponse response = AnimalDrawResponse.builder()
			.animalId(animalId)
			.animalName(animal.getAnimalName())
			.fileUrl(animalMotionRepository.findMotion(animalId).get().getFileUrl())
			.build();

		return response;
	}

	@Override
	public List<FlogAnimalResponse> getFlogAnimalList(String userId) {
		List<UserAnimal> userAnimals = userAnimalRepository.findAllSelectedByUserId(userId).get();
		List<FlogAnimalResponse> responseList = new ArrayList<>();

		for(UserAnimal ua: userAnimals) {
			Long animalId = ua.getAnimal().getAnimalId();

			Optional<Animal> optionalAnimal = animalRepository.findById(animalId);
			Optional<AnimalMotion> optionMotion = animalMotionRepository.findMotion(animalId);

			FlogAnimalResponse response = FlogAnimalResponse.builder()
				.userAnimalName(ua.getUserAnimalName())
				.description(optionalAnimal.get().getDescription())
				.createTime(ua.getTime().getCreateTime())
				.trashTogether(ua.getTrashTogether())
				.lengthTogether(ua.getLengthTogether())
				.timeTogether(ua.getTimeTogether())
				.fileUrl(optionMotion.get().getFileUrl())
				.build();

			responseList.add(response);
		}
		return responseList;
	}

	@Override
	@Transactional
	public void registUserAnimal(MyAnimalRequest request, String userId) {
		//사용자에게 해당 동물이 있는지 판단
		Long animalId = request.getAnimalId();

		userAnimalRepository.findByUserIdAndAnimalId(userId, request.getAnimalId())
			.ifPresentOrElse(userAnimal -> { //이미 존재하는 경우
					UserAnimal ua = userAnimalRepository.findByUserIdAndAnimalId(userId, animalId).get(); //기존꺼 꺼내와서
					ua.setUserAnimalName(request.getUserAnimalName());
					userAnimalRepository.save(ua);
				},
				() -> { //존재하지 않는 경우
					UserAnimal ua = UserAnimal.builder()
						.user(userRepository.findById(userId).get())
						.animal(animalRepository.findById(animalId).get())
						.userAnimalName(request.getUserAnimalName())
						.build();
					userAnimalRepository.save(ua);
				}
			);
	}

	@Override
	public void updateUserAnimal(MyIslandAnimalRequest request, String userId) {

		List<Long> animalIdList = request.getAnimalIdList();

		for(Long id: animalIdList) {
			UserAnimal ua = userAnimalRepository.findByUserIdAndAnimalId(userId, id).get();
			ua.setSelected(true);

			userAnimalRepository.save(ua);
		}

	}


}
