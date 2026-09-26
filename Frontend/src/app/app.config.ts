import { provideHttpClient, withFetch } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideIcons } from '@ng-icons/core';
import {
  lucideArrowDown,
  lucideArrowUp,
  lucideCircleAlert,
  lucideGlobe,
  lucideRefreshCw,
  lucideSearch,
  lucideSearchX,
} from '@ng-icons/lucide';

import { routes } from './app.routes';
import { provideZard } from '@/shared/core/provider/providezard';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withFetch()),
    provideZard(),
    provideIcons({
      lucideArrowDown,
      lucideArrowUp,
      lucideCircleAlert,
      lucideGlobe,
      lucideRefreshCw,
      lucideSearch,
      lucideSearchX,
    }),
  ],
};
