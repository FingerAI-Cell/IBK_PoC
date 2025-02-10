import { StyleSheet } from '@react-pdf/renderer';
import { Font } from '@react-pdf/renderer';
import { vfs } from '../../../fonts/vfs_fonts';

// NanumGothic 폰트 등록
Font.register({
  family: 'NanumGothic',
  fonts: [
    { src: vfs.NanumGothic.normal, fontWeight: 'normal' },
    { src: vfs.NanumGothic.bold, fontWeight: 'bold' }
  ]
});

export const pdfStyles = StyleSheet.create({
  page: {
    padding: 40,
    fontFamily: 'NanumGothic'
  },
  header: {
    marginBottom: 30,
    borderBottom: 2,
    borderColor: '#E8F5E9',
    paddingBottom: 20
  },
  mainTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 20,
    color: '#2C3E50',
    fontFamily: 'NanumGothic'
  },
  infoSection: {
    marginBottom: 10,
    fontSize: 12,
    color: '#666',
    fontFamily: 'NanumGothic'
  },
  infoLabel: {
    fontWeight: 'bold',
    color: '#495057',
    width: 80,
    marginRight: 10,
    fontFamily: 'NanumGothic'
  },
  contentSection: {
    marginTop: 15,
    gap: 10
  },
  // 전체 요약 스타일
  overallSection: {
    marginBottom: 15,
    paddingBottom: 10
  },
  // 화자별 요약 스타일
  speakerSection: {
    marginBottom: 8
  },
  topicSection: {
    marginBottom: 15,
    paddingBottom: 10
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 10,
    marginTop: 10,
    color: '#2C3E50',
    paddingBottom: 6,
    borderBottom: 1,
    borderColor: '#E8F5E9',
    fontFamily: 'NanumGothic'
  },
  content: {
    fontSize: 11,
    lineHeight: 1.4,
    marginBottom: 6,
    color: '#4A4A4A',
    fontFamily: 'NanumGothic'
  },
  speakerName: {
    fontSize: 12,
    fontWeight: 'bold',
    color: '#2C3E50',
    marginBottom: 4,
    fontFamily: 'NanumGothic'
  }
}); 