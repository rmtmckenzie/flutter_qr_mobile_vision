import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:qr_mobile_vision/qr_mobile_vision.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('heartbeat test', (WidgetTester tester) async {
    await QrMobileVision.heartbeat();
  });
}
