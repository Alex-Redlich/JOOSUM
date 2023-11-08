import React, {useState, useEffect, useRef} from 'react';

import {
  View,
  Image,
  TouchableOpacity,
  ImageBackground,
  AppState,
} from 'react-native';
import {PloggingScreenProps} from 'typePath';
import {NewData, TrashList, TrashDaTaList} from '@/types/plogging';
import TrashModal from '@/components/ui/Modal/TrashModal';
import {styles} from './styles';
import AppText from '@/components/ui/Text';
import AppButton from '@/components/ui/Button';
import GoogleMap from '@/components/ui/Map/GoogleMap';
import ViewShot from 'react-native-view-shot';
// import CameraRoll from '@react-native-community/cameraroll';
// import {DATA} from './TrashImageList';
import PloggingResultModal from '@/components/ui/Modal/PloggingResultModal';
// import {StyleSheet} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import {storeImage} from '../CameraPage/savePhoto';
interface ActivityDataType {
  activityImg: string; // 이미지에 대한 타입을 가정
  activityRequestDto: {
    length: number;
    time: number;
    trash: number;
  };
}

export default function PloggingPage({navigation, route}: PloggingScreenProps) {
  // 모달 관리 값
  const [isEndModalVisible, setIsEndModalVisible] = useState(false);
  const [isModalVisible, setModalVisible] = useState(false);
  const [trashData, setTrashData] = useState<TrashDaTaList>();
  const [getAnimalIMG, setGetAnimalIMG] = useState<string>('');
  const [getAnimalID, setGetAnimalID] = useState<number>(0);

  // 종료 여부
  let endPlog: boolean = false;
  // 모달 여는 부분. params로 함수 받아와서 그 값에 따라 모달 연다.

  useEffect(() => {
    const unsubscribe = navigation.addListener('blur', () => {
      // 페이지에서 벗어날 때(다른 페이지로 이동할 때) 모달 상태를 false로 설정
      setModalVisible(false);
    });

    // 리스너 정리
    return unsubscribe;
  }, [navigation]);

  useEffect(() => {
    if (route.params?.shouldOpenModal === true) {
      setModalVisible(true);
    }

    if (route.params.TrashData) {
      setTrashData(route.params.TrashData);
      console.log('플로깅에서 쓰레기를 받음', route.params.TrashData);
      // console.log('그걸 새로 저장함', trashData);
    }

    if (route.params.selectedAnimalIMG) {
      setGetAnimalIMG(route.params.selectedAnimalIMG);
      // console.log('이미지를 받음', getAnimalIMG);
    }

    if (route.params.selectedAnimalID) {
      setGetAnimalID(route.params.selectedAnimalID);
      // console.log('정령 ID 받음', getAnimalID);
    }
  }, [route.params]);

  useEffect(() => {
    if (trashData) {
      console.log('trashData가 업데이트됨:', trashData);
    }
  }, [trashData]);

  // useEffect(() => {
  //   console.log('이미지를 받음', getAnimalIMG);
  //   console.log('아이디도', getAnimalID);
  // }, [getAnimalIMG]);

  const [resultData, setResultData] = useState<TrashList[]>();
  const [ploggingDistance, setPloggingDistance] = useState(0);
  const [trashCount, setTrashCount] = useState(100);
  const [trashImage, setTrashImage] = useState('');
  const [timer, setTimer] = useState<number>(0);
  const [activityData, setActivityData] = useState<ActivityDataType>();

  // --------------------------------------------  타이머 기능을 위한 변수  --------------------------------------------

  // 타이머 기능을 위한 값
  const [appState, setAppState] = useState(AppState.currentState);
  const [backgroundTime, setBackgroundTime] = useState<number | null>(null);

  //컴포넌트의 전체 라이프 사이클에 영향없는 시간 값 만들기
  let intervalRef = useRef<number | null>(null);
  const handleAppStateChange = (nextAppState: typeof appState) => {
    if (appState.match(/inactive|background/) && nextAppState === 'active') {
      // 앱이 백그라운드에서 포그라운드로 전환될 때
      const currentTime = Date.now();
      if (backgroundTime) {
        const diffTime = Math.floor((currentTime - backgroundTime) / 1000); // 초 단위
        setTimer(prevTimer => prevTimer + diffTime);
      }
    } else if (
      nextAppState.match(/inactive|background/) &&
      appState === 'active'
    ) {
      // 앱이 포그라운드에서 백그라운드로 전환될 때
      setBackgroundTime(Date.now());
    }
    setAppState(nextAppState);
  };

  // 사용자의 앱 사용 상태에 따른 타이머 기능 함수
  useEffect(() => {
    // 앱이 포그라운드 상태일 때 타이머 시작
    if (appState === 'active') {
      intervalRef.current = setInterval(() => {
        setTimer(prevTimer => prevTimer + 1);
      }, 1000) as unknown as number;
    }

    const appStateSubscription = AppState.addEventListener(
      'change',
      handleAppStateChange,
    );

    return () => {
      // 컴포넌트가 언마운트되거나 앱 상태가 변경될 때 타이머 정지
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
      appStateSubscription.remove();
    };
  }, [appState]);

  // useEffect(() => {
  //   console.log(trashImage, '플로깅 페이지에서 업데이트 된 쓰레기 이미지');
  //   // console.log('타이머가 왜 안될까🖤', activityData);
  // }, [trashImage]);

  // 시간 포맷 맞추기 위한 상수. 추후 옮길 것
  const formatTime = (time: number) => {
    const hours = Math.floor(time / 3600);
    const minutes = Math.floor((time % 3600) / 60);
    const seconds = time % 60;

    return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(
      2,
      '0',
    )}:${String(seconds).padStart(2, '0')}`;
  };

  //플로깅 완료 시 작동될 로직.

  // --------------------------------------------  스크린샷 기능을 위한 변수  --------------------------------------------

  const captureRef = useRef<ViewShot | null>(null);
  const now = new Date();
  const fileName = `plog-${now.getFullYear()}-${
    now.getMonth() + 1
  }-${now.getDate()}-${now.getHours()}-${now.getMinutes()}-${now.getSeconds()}`;

  const getPhotoUri = async (): Promise<string> => {
    if (!captureRef.current) {
      console.log('captureRef is null or undefined');
      return '';
    } else if (!captureRef.current.capture) {
      console.log('captureRef is null or undefined');
      return '';
    } else {
      const uri = await captureRef.current.capture();
      console.log('👂👂 Image saved to', uri);
      return uri;
    }
  };

  // 스크린샷 찍기
  const onCapture = async () => {
    try {
      const uri = await getPhotoUri();
      if (uri) {
        const storedImagePath = await storeImage(uri);
        if (storedImagePath) {
          setTrashImage(storedImagePath); // 새 경로로 상태 업데이트
          console.log(`Image stored at: ${storedImagePath}`);
        } else {
          console.log('Failed to obtain stored image path');
        }
      }
    } catch (e) {
      console.log('😻😻😻 snapshot failed', e);
    }
  };

  /* 
  // Android 저장 요청
  const hasAndroidPermission = async () => {
    const permission = PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE;
    const hasPermission = await PermissionsAndroid.check(permission);
    if (hasPermission) {
      return true;
    }
    const status = await PermissionsAndroid.request(permission);
    return status === 'granted';
  };

  // 갤러리 저장
  const onSave = async () => {
    if (Platform.OS === 'android' && !(await hasAndroidPermission())) {
      console.log('갤러리 접근 권한이 없어요');
      return;
    }

    const uri = await getPhotoUri();
    const result = await CameraRoll.save(uri);
    console.log('갤러리 result', result);
  };
  */

  const stopAndResetTimer = async () => {
    // 플로깅 종료 신호 넘겨주기
    endPlog = false;

    // 타이머 멈추기
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
    }

    await onCapture(); // 스크린샷 찍기
    await loadImage();
  };
  const loadImage = async () => {
    try {
      const imagePath = await AsyncStorage.getItem('@photo_path');
      if (imagePath !== null) {
        setTrashImage(imagePath);
      }
    } catch (e) {
      // 로딩 에러 처리
      console.error('Failed to load the photo path.', e);
    }
  };
  useEffect(() => {
    const newActivityData = {
      activityImg: trashImage,
      activityRequestDto: {
        length: ploggingDistance,
        time: timer,
        trash: trashCount,
      },
    };

    const newResultData = [
      {
        img: require('@/assets/img_icon/sand_clock_icon.png'),
        title: formatTime(timer),
      },
      {
        img: require('@/assets/img_icon/trash_icon.png'),
        title: `${trashCount} 개`,
      },
      {
        img: require('@/assets/img_icon/shoe_icon.png'),
        title: `${ploggingDistance} km`,
      },
    ];

    if (trashImage) {
      setResultData(newResultData);
      setActivityData(newActivityData);
      setTimer(0);
      setTrashCount(0);
      setPloggingDistance(0);
      setIsEndModalVisible(true);
    }
  }, [trashImage]);

  const resultNav = (newData: NewData) => {
    navigation.navigate('PloggingResult', {
      resultList: resultData,
      activityData: activityData,
      newData: newData, // the new data received from the mutation onSuccess
    });
  };

  const closeModalAndUpdateCount = () => {
    // Modal 닫기
    setModalVisible(false);

    // trashData.total 값을 trashCount에 추가
    if (trashData && trashData.total) {
      setTrashCount(prevCount => prevCount + trashData.total);
    }
  };

  const DATA = [
    {
      title: '일반 쓰레기',
      img: require('@/assets/img_icon/normal_trash.png'),
      description: trashData?.general_trash || 0,
    },
    {
      title: '플라스틱',
      img: require('@/assets/img_icon/plastic_trash.png'),
      description: trashData?.plastic || 0,
    },
    {
      title: '캔',
      img: require('@/assets/img_icon/can_trash.png'),
      description: trashData?.metal || 0,
    },
    {
      title: '종이',
      img: require('@/assets/img_icon/paper_trash.png'),
      description: trashData?.paper || 0,
    },
    {
      title: '유리',
      img: require('@/assets/img_icon/glass_bottle_trash.png'),
      description: trashData?.glass || 0,
    },
    {
      title: '비닐 봉투',
      img: require('@/assets/img_icon/plastic_bag_trash.png'),
      description: trashData?.plastic_bag || 0,
    },
  ];
  // console.log('이걸 주고 있거든여', DATA);

  // 쓰레기 사진 찍었을 때, 카메라로 이동하고, 찍었다는 신호를 지도에 전달
  const captureTrashCount = useRef<number>(0);
  const captureTrash = () => {
    captureTrashCount.current += 1;
    navigation.navigate('Camera');
  };

  return (
    <View style={{flex: 1}}>
      <TrashModal
        isVisible={isModalVisible}
        onClose={closeModalAndUpdateCount}
        data={DATA}
        navigation={navigation}
      />
      <PloggingResultModal
        isVisible={isEndModalVisible}
        onClose={() => setIsEndModalVisible(false)}
        data={resultData}
        activityData={activityData}
        navigation={resultNav}
      />

      <View style={styles.container}>
        <View style={styles.topContainer}>
          <AppButton children="플로깅 완료하기" onPress={stopAndResetTimer} />
        </View>
        <ImageBackground
          style={styles.bottomContainer}
          source={require('@/assets/plogingpage_image/Background.png')}
          resizeMode="contain">
          <View style={styles.textContainer}>
            <AppText style={styles.text}>{ploggingDistance}km</AppText>
            <AppText style={styles.text}>{formatTime(timer)}</AppText>
            <AppText style={styles.text}>{trashCount}개</AppText>
          </View>

          <TouchableOpacity style={styles.cameraBtn} onPress={captureTrash}>
            <Image
              source={require('@/assets/plogingpage_image/cameraBtn.png')}
            />
          </TouchableOpacity>
        </ImageBackground>
        {/* 지도 import */}
        <ViewShot
          style={styles.mapContainer}
          ref={captureRef}
          options={{fileName: fileName, format: 'jpg', quality: 0.9}}>
          <GoogleMap
            endPlog={endPlog}
            animalImg={getAnimalIMG}
            trashCount={trashCount}
            setPloggingDistance={setPloggingDistance}></GoogleMap>
        </ViewShot>
      </View>
    </View>
  );
}
