import angular from 'angular';
import './media-api';
import '../../services/image-list';

import {service} from '../../edits/service';
import { trackAll } from '../../util/batch-tracking';
import { getApiImageAndApiLeasesIfUpdated } from './leases-helper';
import { Subject } from 'rx';

var leaseService = angular.module('kahuna.services.lease', [
  service.name
  ]);

leaseService.factory('leaseService', [
    '$rootScope',
    '$q',
    'imageAccessor',
    'imageList',
    'mediaApi',
    'editsService',
    'apiPoll',
    function ($rootScope, $q, imageAccessor, imageList, mediaApi, editsService, apiPoll) {
        let leasesRoot;
        function getLeasesRoot() {
            if (!leasesRoot) {
                leasesRoot = mediaApi.root.follow('leases');
            }
            return leasesRoot;
        }

        function getLeases(images) {
            // search page has fancy image list
            if (angular.isDefined(images.toArray)) {
                images = images.toArray();
            }
            return $q.all(images.map(i => i.get()))
                .then((images) => imageList.getLeases(images));
        }

        function clear(image) {
            return refreshImages([image]).then(images => {
                const currentLeases = getLeases(images);
                return currentLeases.then(() => {
                    return image
                        .perform('delete-leases').then(() => {
                            pollLeasesAndUpdateUI(images, imageList.getLeases(images));
                        });
                });
            });
        }

        function replace(image, leases) {
            const images = [image];
            const currentLeasesPromise = getLeases(images);

            return currentLeasesPromise.then((currentLeases) => {

                const updatedLeases = leases.map((lease) => {
                    var newLease = angular.copy(lease);
                    newLease.mediaId = image.data.id;
                    return newLease;
                });

                // Don't update the leases if they're "the same"
                if (JSON.stringify(currentLeases.leases) === JSON.stringify(updatedLeases)) {
                    return image;
                }

                return image
                    .perform('replace-leases', { body: updatedLeases })
                    .then(() => pollLeasesAndUpdateUI(images));
            });
        }

        function add(image, lease) {
            const newLease = angular.copy(lease);
            newLease.mediaId = image.data.id;

            if (angular.isDefined(newLease.notes) && newLease.notes.trim().length === 0) {
                newLease.notes = null;
            }
            return image.perform('add-lease', { body: newLease });
        }

        function batchAdd(lease, images) {
            //We check whether the leases in the image have a later lastModified date,
            //so make sure that we don't have anything in the pipeline.
                        // search page has fancy image list
                        if (angular.isDefined(images.toArray)) {
                            images = images.toArray();
                        }
            console.log(images);
            return refreshImages(images).then(updatedImages => {
                console.log(images);
                console.log(images.map);
                return trackAll($q, $rootScope, "leases", updatedImages, [
                    image => add(image, lease),
                    image => apiPoll(() => untilLeasesChange([image])).then(([{ image }]) => image) //Extract the image from untilLeasesChange
                ], ['images-updated', 'leases-updated'])
            }
            );
    }

    function canUserEdit(image){
      return editsService.canUserEdit(image);
    }

    /**
     * Delete a lease by uuid from a collection of images.
     * This method does not support batch deletion, because a
     * uuid will only ever match one lease.
     */
        function deleteLease(lease, images) {
                        // search page has fancy image list
                        if (angular.isDefined(images.toArray)) {
                            images = images.toArray();
                        }
        return refreshImages(images).then(images =>
       getLeasesRoot().follow('leases', {id: lease.id}).delete()
                .then(() => pollLeasesAndUpdateUI(images)));
    }

    function getByMediaId(image) {
      return getLeasesRoot().follow('by-media-id', {id: image.data.id}).get();
    }

    function pollLeasesAndUpdateUI(images) {
      apiPoll(() => {
        return untilLeasesChange(images);
      }).then(results => {
          console.log(results);
          return results.map(({ image, leases }) => {
              emitLeaseUpdate(leases);
              console.log(images, leases);
                emitImageUpdate(image);
          });
      });
    }

    const imageUpdates$ = new Subject();
        imageUpdates$.bufferWithTime(1000).subscribe((images) => {
            if (images.length == 0) {
                return;
            }
            console.log(images);
            $rootScope.$emit('images-updated', images);
        });

    function emitImageUpdate(lease) {
        imageUpdates$.onNext(lease);
    }
    const leaseUpdates$ = new Subject();
        leaseUpdates$.bufferWithTime(1000).subscribe((leases) => {
            if (leases.length == 0) { return; }
            console.log("DUCK", leases, leases.length);
            $rootScope.$emit('leases-updated');
    });

    function emitLeaseUpdate(lease) {
        leaseUpdates$.onNext(lease);
    }

      //This is a race condition, but I think it's a rare one.
        //Mitigate by refreshing images with refreshImages before posting.
    function untilLeasesChange(images) {
      const imagesArray = images.toArray ? images.toArray() : images;
      return $q.all(imagesArray.map(image => {
        return image.get().then(apiImage => {
          const apiImageAndApiLeases = getApiImageAndApiLeasesIfUpdated(image, apiImage);
          if (apiImageAndApiLeases) {
              image = apiImage;
              console.log(apiImageAndApiLeases);
            return apiImageAndApiLeases;
          } else {
            // returning $q.reject() will make apiPoll function to poll again
            // until api call will return image with updated leases
            return $q.reject();
          }
        });
      }));
    }

        // untilLeasesChange checks lastModified, make sure we always have the latest image before starting
    function refreshImages(images) {
        const x = $q.all(images.map(_ => _.get()));
        console.log(x);
        return x;
    }

    function flattenLeases(leaseByMedias) {
      return {
        leases: leaseByMedias.map(l => l.leases).reduce((a, b) => a.concat(b)),
        lastModified: leaseByMedias.map(l => l.lastModified).sort()[0]
      };
    }

    function isLeaseSyndication(lease) {
      return lease.access.endsWith('-syndication');
    }

    return {
        batchAdd,
        getLeases,
        canUserEdit,
        deleteLease,
        getByMediaId,
        replace,
        clear,
        flattenLeases,
        isLeaseSyndication
    };
}]);

export default leaseService;
