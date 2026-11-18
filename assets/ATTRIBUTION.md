# 실제 사용 자산 및 출처

2026-10-29 후속 작업에서 소개용 원화 대신 인게임 도트 스프라이트를 다운로드해 적용했습니다.

출처: [visolleon/bnb Pic 폴더](https://github.com/visolleon/bnb/tree/gh-pages/Pic). GitHub 파일 연결의 Base64 응답을 PNG로 복원했습니다. 파일별 Git blob SHA는 [provenance.json](provenance.json)에 기록했습니다. 출처 저장소 프로그램은 실행하지 않았습니다.

| 파일 | 크기 | 사용 |
|---|---|---|
| Role1.png | 288×256 | 배찌, 48×64 셀, 4방향 6프레임 |
| Role2.png | 336×268 | 로두마니, 56×67 셀, 4방향 6프레임 |
| Role1Die.png / Role2Die.png | 528×100 / 616×98 | 탈락 잔상 |
| BigPopo.png | 648×72 | 물방울 프레임을 감금 캐릭터에 합성 |
| Popo.png | 132×41 | 물풍선 3프레임 |
| TownGround.png | 280×40 | 첫 번째 빌리지 바닥 |
| TownBox.png / TownBlockRed.png | 40×44 | 파괴 상자 / 고정 블록 |
| Explosion.png | 560×200 | 확보·보관만. 현재 물줄기는 자체 그래픽 |

이미지 권리는 원권리자에게 있습니다. 공개 저장소에서 접근 가능함을 확인했지만 자유 배포·상업 이용 라이선스를 확인한 것은 아닙니다. 저장소 코드의 라이선스와 원작 게임 이미지 권리는 별개입니다.

## 크롭

원본 픽셀과 투명도를 보존합니다. AI 재생성이나 원화의 도트 변환은 하지 않았습니다.

- 원본 행: 위 / 아래 / 왼쪽 / 오른쪽.
- 게임 방향: 아래 / 왼쪽 / 오른쪽 / 위. 행 매핑: 1, 2, 3, 0.
- 이동 중 6프레임 반복, 정지 시 프레임 정지. 로비에서도 걷기 미리보기.
- assets/cropped: 원본 해상도 프레임 48장과 contact-sheet.png.
- assets/characters.png: 두 캐릭터 합성 시트, 576×768.
- assets/trapped.png: 물방울 합성 시트, 576×192.
- 재생성: sh run.sh --crop-sprites assets/cropped
- 합성 시트 재생성: sh run.sh --export-art assets

UI, 포레스트·캔디 테마, 물줄기·파편, 일부 아이템은 자체 Java2D 그래픽입니다. 효과음은 아래 다운로드 음원으로 교체했습니다. 다오 등 나머지 캐릭터는 아직 미포함입니다.

## 오디오 업데이트 (2026-10-30)

12개 WAV를 다운로드해 `assets/audio/`와 실행 JAR에 포함했습니다. 파일별 원본 주소와 Git blob SHA는 `assets/audio/provenance.json`에 있습니다. 원본 파일은 변환하지 않고 보관하며 Java 재생 시 PCM 44.1kHz/16bit로 디코딩합니다.

| 파일 | 이벤트 | 출처 |
|---|---|---|
| lay.wav | 물풍선 설치 성공 | visolleon/bnb Snd |
| explode.wav | 폭발·연쇄 폭발 | 동일 |
| get.wav | 아이템 획득 | 동일 |
| appear.wav | 물방울 감금 (이 프로젝트의 매핑) | 동일 |
| save.wav | 팀원 구출·바늘 탈출 | 동일 |
| die.wav | 탈락 | 동일 |
| start.wav | 카운트다운 종료 | 동일 |
| win.wav / draw.wav | 승리 / 무승부 | 동일 |
| bg.wav | 로비·경기 BGM, 30초 반복 | 동일 |
| lose.wav | 패배 | NPpj/CrazyArcade GameClient/src/assets/sound |
| click.wav | 메뉴 클릭 | 동일 |

음원 출처: [visolleon/bnb Snd](https://github.com/visolleon/bnb/tree/gh-pages/Snd), [NPpj/CrazyArcade sound](https://github.com/NPpj/CrazyArcade/tree/master/GameClient/src/assets/sound). 정확한 곡명과 원작 각 버전의 동일 이벤트 매핑까지 확인한 것은 아닙니다. 공개 저장소에서 받았다는 사실이 자유 배포·상업 이용 허가를 의미하지 않습니다.

효과음은 종류별 Clip을 재사용해 서로 겹쳐 재생합니다. 같은 처리 주기에 몰린 동일 효과는 합쳐 음량 폭주를 줄입니다. BGM -16dB, 일반 효과 -7dB, 클릭 -14dB. 일시정지 시 음악/효과음 정지, 재개 시 음악 이어 재생, 결과 화면에서는 음악 정지 후 결과 효과음만 재생합니다. M 또는 소리 버튼으로 현재 출력까지 음소거하며 대기 중 소리를 버립니다.

검증: 12개 WAV 전체 디코딩, 이벤트 회귀 테스트, 다른 작업 디렉터리에서 JAR 내 음원 로딩. 현재 도구 실행 환경은 Java 오디오 출력 장치를 제공하지 않아 실청취는 미검증입니다. 로컬 재생 확인: `sh run.sh --audio-smoke`.

## 조사했지만 사용하지 않은 자료

## 아이템 도트 업데이트 (2026-10-30)

물풍선 수·물줄기·속도·바늘 4종을 실제 다운로드 이미지로 교체했습니다. 기능 및 드랍 확률은 유지했습니다.

| 게임 아이템 | 이미지 | 처리 |
|---|---|---|
| BALLOON / 설치 수 증가 | visolleon/bnb Pic/Gift1.png | 42×45 셀, 3프레임 |
| SPEED / 속도 증가 | visolleon/bnb Pic/Gift2.png | 롤러스케이트, 42×45 셀, 3프레임 |
| POWER / 물줄기 증가 | visolleon/bnb Pic/Gift3.png | 파란 물약, 42×45 셀, 3프레임 |
| NEEDLE / 감금 탈출 | tpoisonooo/BnB images의 아이템 창 PNG | (171,150), 26×25 크롭. 원래 회색 슬롯 배경 보존 |

출처: [visolleon/bnb Pic](https://github.com/visolleon/bnb/tree/gh-pages/Pic), [tpoisonooo/BnB images](https://github.com/tpoisonooo/BnB/tree/master/images). 각 파일의 URL, SHA와 크롭 좌표는 `assets/items-provenance.json`에 기록했습니다. 종류 매핑은 원본 데모의 아이템 동작과 이미지를 함께 확인했습니다. 3프레임 아이템은 0.4초마다 전환하고 떠 있는 효과를 더했습니다. 바늘은 정지 이미지입니다.

`client/assets/items/needle.png` 후보는 육안 확인 결과 잘못된 크롭이어서 사용하지 않았습니다. 미사용 Gift4~6 및 바늘 후보는 `build/item-candidates`에 보관하며 JAR에 포함하지 않습니다.

크롭 재생성: `sh run.sh --crop-items assets/cropped-items`. 결과: 개별 PNG 12장(바늘은 같은 이미지 3장)과 `items-preview.png`. 맵/우측 보유 슬롯 모두 같은 렌더러를 사용합니다. 원작 자산의 자유 배포·상업 이용 허가를 확인한 것은 아닙니다.

- [헝그리앱 소개 그림](https://www.hungryapp.co.kr/bbs/bbs_view.php?bcode=ca&pid=2), [Zerochan 다오 원화](https://www.zerochan.net/2675838): 인게임 도트 요청에 따라 미사용.
- [NPpj/CrazyArcade](https://github.com/NPpj/CrazyArcade): 배찌 방향별 파일은 미사용. 효과음과 아래 경기 프레임은 사용.
- [shalakar/CrazyArcade](https://github.com/shalakar/CrazyArcade): 일부 후보 확인 후 제외. build/asset-research에 보관, JAR 제외.
- [Kenney Sokoban](https://kenney.nl/assets/sokoban): 초기 대체 후보, 미사용.

## 경기 UI 프레임

`assets/original/GameBackGround.png`는 [NPpj/CrazyArcade의 경기 배경](https://github.com/NPpj/CrazyArcade/blob/master/GameClient/src/assets/game/GameBackGround.png)에서 다운로드했습니다. 1020×930 이미지를 800×600으로 변환 표시하고 경기장·타이머·참가자·아이템을 위에 그립니다. 파일 SHA는 `assets/ui-provenance.json`, UI·애니메이션 참고값은 `docs/CLASSIC_UI.md`에 기록했습니다. 공개 저장소에 있다는 사실은 원작 자산의 재배포 허가를 뜻하지 않습니다.
